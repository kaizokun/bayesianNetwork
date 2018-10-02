package decision;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import environment.DirectionMove;
import environment.Position;
import environment.SimpleMap;
import javafx.geometry.Pos;
import math.Distribution;
import network.Variable;

import java.util.*;

import static java.util.Arrays.asList;
import static network.factory.SimpleMapRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.SimpleMapRDDFactory.SIMPLE_MAP_VARS.POSITION;
import static network.factory.SimpleMapRDDFactory.SIMPLE_MAP_VARS.WALL_PERCEPT;

public class PDMPOSimpleMap implements PDMPO {

    protected SimpleMap simpleMap;

    protected IDomain actionDomain, perceptDomain, stateDomain;

    protected AbstractDoubleFactory doubleFactory;

    protected Variable actionVar, perceptVar;

    protected List<Variable> actionVars, stateVars;

    protected Map<Position, AbstractDouble> statesUtility = new Hashtable<>();

    protected Map<Position, Set<Domain.DomainValue>> positionsMoves = new Hashtable<>();

    public PDMPOSimpleMap(SimpleMap simpleMap, AbstractDoubleFactory doubleFactory) {

        this.simpleMap = simpleMap;

        this.actionDomain = DomainFactory.getDirectionMoveDomain();

        this.perceptDomain = DomainFactory.getMazeWallCaptorDomain();
        //normalement l'ordre des positions est identiques que dans le RDB
        this.stateDomain = DomainFactory.getPositionsDomain(simpleMap.getAllStates());
        //sert à initialiser les variabels du reseau apres extension
        this.actionVar = new Variable(MOVE, actionDomain);

        this.perceptVar = new Variable(WALL_PERCEPT, perceptDomain);
        //sert au forward pour la requete et la liste des variables actions à ignorer
        //lors de la construction de la distribution
        this.actionVars = asList(new Variable(MOVE));

        this.stateVars = asList(new Variable(POSITION));
        //fabrique de double ou bigdecimal
        this.doubleFactory = doubleFactory;

        this.initStatesUtility();

        this.initPositionsMoves();
    }

    private void initPositionsMoves() {

        //pour chaque état non final
        for(Position position : simpleMap.getNotFinalStates()){

            //crée un ensemble de deplacement
            Set<Domain.DomainValue> positionMoves = new HashSet<>();

            //pour chaque deplacement possible
            for (DirectionMove move : DirectionMove.getMoves()) {
                //deplace la position
                Position nextPosition = position.move(move);
                //si deplacement possible
                if (simpleMap.isPositionReachable(nextPosition)) {
                    //ajout de l'objet DomainValue encapsulant la direction dans le set
                    positionMoves.add(actionDomain.getDomainValue(move));
                }
            }

            positionsMoves.put(position, positionMoves);
        }
    }

    private void initStatesUtility() {

        for (Position position : simpleMap.getNotFinalStates()) {

            statesUtility.put(position, doubleFactory.getNew(-0.04));
        }

        statesUtility.put(simpleMap.getGoodExit(), doubleFactory.getNew(1.0));

        statesUtility.put(simpleMap.getBadExit(), doubleFactory.getNew(-1.0));
    }

    @Override
    public AbstractDouble getUtility(Distribution forward) {

        AbstractDouble utility = doubleFactory.getNew(0.0);
        //pour chaque position
        for (Domain.DomainValue value : forward.getRowValues()) {

            Position position = (Position) value.getValue();
            //multiplie la probabilité de la position par son utilité
            //et ajoute chaque utilité pondérée à la somme finale
            utility = utility.add(statesUtility.get(position).multiply(forward.get(value)));
        }

        return utility;
    }

    @Override
    public List<Variable> getStates() {

        return this.stateVars;
    }

    @Override
    public List<Variable> getActions() {

        return this.actionVars;
    }

    @Override
    public Variable getActionVar() {

        return this.actionVar;
    }

    @Override
    public Variable getPerceptVar() {

        return this.perceptVar;
    }

    @Override
    public Domain.DomainValue getNoAction() {
        return this.actionDomain.getDomainValue(DirectionMove.ON_THE_SPOT);
    }

    /*
     * retourne une liste d'acitons possible depuis un etat
     * si plusieurs action son pourraient avoir des combinaisons d'actions
     * DomainValue peut être utilisé de manière composite avec une Megavariable
     * !! l'ordre de DomainValues action doit correspondre à celui des sous variables si Megavariable
     * retourné par getActionVar
     * */


    @Override
    public Set<Domain.DomainValue> getActionsFromState(Distribution forward, AbstractDouble minProb) {

        Set<Domain.DomainValue> actions = new HashSet<>();

        //pour chaque valeur ou combinaison de valeur d'état
        for (Domain.DomainValue value : forward.getRowValues()) {
            //pour les états probables > 0
            if (forward.get(value).compareTo(minProb) > 0) {
                //recupere l'object Position stocké dans la valeur de domaine
                Position position = (Position) value.getValue();
                //deplacement impossible depuis les positions finales
                if (!simpleMap.isFinalState(position)) {
                    //ajout des actions possible (précalculées) depuis la position
                    actions.addAll(positionsMoves.get(position));
                }
            }
        }

        return actions;
    }

    protected Map<String, Collection<RsState>> stateActionResultMap = new Hashtable<>();

    @Override
    public Collection<RsState> getResultStates(Domain.DomainValue state, Domain.DomainValue action) {

        String stateActionKey = state.toString() + "." + action.toString();

        if (stateActionResultMap.containsKey(stateActionKey)) {

            return stateActionResultMap.get(stateActionKey);
        }

        //map permetant de savoir si une position resultat est déja enregistré
        //dans quel cas sa probabilité doit augmenter si plusieurs moyens d'access
        Map<Position, RsState> rsStates = new Hashtable<>();

        Position positionOrigin = (Position) state.getValue();

        //si position finale l'action à 100% chance de rester au même endroit
        if (this.simpleMap.isFinalState(positionOrigin)) {

            return Arrays.asList(new RsState(state, doubleFactory.getNew(1.0)));
        }

        DirectionMove move = (DirectionMove) action.getValue();

        this.addNewPosition(rsStates, positionOrigin, move, 0.8);

        this.addNewPosition(rsStates, positionOrigin, move.getRelativeRight(), 0.1);//v

        this.addNewPosition(rsStates, positionOrigin, move.getRelativeLeft(), 0.1);

        stateActionResultMap.put(stateActionKey, rsStates.values());

        return rsStates.values();
    }

    private void addNewPosition(Map<Position, RsState> rsStates,
                                Position positionOrigin,
                                DirectionMove direction,
                                double prob) {

        //deplace la position dans la direction
        Position nextPosition = positionOrigin.move(direction);
        //si position n'est pas accessible dans le labyrinthe
        //on fait du sur place
        if (!simpleMap.isPositionReachable(nextPosition)) {

            nextPosition = positionOrigin;
        }

        //si l'état resultat a déja été enregistré
        if (rsStates.containsKey(nextPosition)) {

            RsState rsStateSaved = rsStates.get(nextPosition);
            //on ajoute la probabilité calculé pour une autre manière d'y acceder
            rsStateSaved.setProb(rsStateSaved.getProb().add(doubleFactory.getNew(prob)));

        } else {
            //sinon on se contente de l'ajouter dans la map
            rsStates.put(nextPosition, new RsState(stateDomain.getDomainValue(nextPosition), doubleFactory.getNew(prob)));
        }
    }

    /*
     * !! l'ordre de percept dans le Domainevalue composite  doit correspondre à celui des sous variables percepts
     * si Megavariable retourné par getPerceptVar()
     * */
    @Override
    public Domain.DomainValue getPerceptFromState(Domain.DomainValue state) {
        //la classe simple map fourni le percept à partir de la map et de la position
        return perceptDomain.getDomainValue(this.simpleMap.getPercept((Position) state.getValue()));
    }

    @Override
    public boolean isFinalState(Domain.DomainValue state) {

        return this.simpleMap.isFinalState((Position) state.getValue());
    }

    @Override
    public AbstractDouble getProbRightPercept(Domain.DomainValue percept) {

        return doubleFactory.getNew(0.6);
    }
}
