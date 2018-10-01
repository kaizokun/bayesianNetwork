package decision;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import environment.Cardinal;
import environment.DirectionMove;
import environment.Position;
import environment.SimpleMap;
import javafx.geometry.Pos;
import math.Distribution;
import network.Variable;

import java.util.*;

import static java.util.Arrays.asList;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.POSITION;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.WALL_PERCEPT;

public class PDMPOSimpleMap implements PDMPO {

    protected SimpleMap simpleMap;

    protected IDomain actionDomain, perceptDomain, stateDomain;

    protected AbstractDoubleFactory doubleFactory;

    protected Variable actionVar, perceptVar;

    protected List<Variable> actionVars, stateVars;

    protected Map<Position, AbstractDouble> statesUtility = new Hashtable<>();

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
    }

    private void initStatesUtility() {

        for(Position position : simpleMap.getNotFinalStates()){

            statesUtility.put(position, doubleFactory.getNew(-0.04));
        }

        statesUtility.put(simpleMap.getGoodExit(), doubleFactory.getNew(1.0));

        statesUtility.put(simpleMap.getBadExit(), doubleFactory.getNew(-1.0));
    }

    @Override
    public AbstractDouble getUtility(Distribution forward){

        AbstractDouble utility = doubleFactory.getNew(0.0);
        //pour chaque position
        for(Domain.DomainValue value : forward.getRowValues()){

            Position position = (Position) value.getValue();
            //multiplie la probabilité de la position par son utilité
            //et ajoute chaque utilité pondérée à la somme finale
            utility = utility.add( statesUtility.get(position).multiply(forward.get( value )));
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

    /*
     * retourne une liste d'acitons possible depuis un etat
     * si plusieurs action son pourraient avoir des combinaisons d'actions
     * DomainValue peut être utilisé de manière composite avec une Megavariable
     * !! l'ordre de DomainValues action doit correspondre à celui des sous variables si Megavariable
     * retourné par getActionVar
     * */
    @Override
    public Set<Domain.DomainValue> getActionsFromState(Distribution forward) {

        Set<Domain.DomainValue> actions = new HashSet<>();

        //pour chaque valeur ou combinaison de valeur d'état
        for (Domain.DomainValue value : forward.getRowValues()) {
            //pour les états probables > 0
            if (forward.get(value).getDoubleValue().compareTo(0.0) > 0) {
                //recupere l'object position stocké dans la valeur de domaine
                Position position = (Position) value.getValue();
                //pour chaque direction alentours
                for (DirectionMove move : DirectionMove.values()) {

                    Position nextPosition = position.move(move);

                    if (simpleMap.isPositionReachable(nextPosition)) {

                        actions.add(actionDomain.getDomainValue(move));
                    }
                }
            }
        }

        return actions;
    }

    @Override
    public Collection<RsState> getResultStates(Domain.DomainValue state, Domain.DomainValue action) {

        Map<Position, RsState> rsStates = new Hashtable<>();

        Position position = (Position) state.getValue();

        DirectionMove direction = (DirectionMove) action.getValue();

        this.addNewPosition(rsStates, position, direction, 0.8);

        this.addNewPosition(rsStates, position, direction.getRelativeRight(), 0.1);

        this.addNewPosition(rsStates, position, direction.getRelativeLeft(), 0.1);

        return rsStates.values();
    }

    private void addNewPosition(Map<Position, RsState> rsStates, Position position, DirectionMove direction, double prob) {
        //deplace la position dans la direction
        Position moveStraight = position.move(direction);
        //si position accessible dans le labyrinthe
        if (simpleMap.isPositionReachable(moveStraight)) {

            position = moveStraight;
        }
        //resultat du deplacement avec probabilité pour action non deterministe
        RsState rsState = new RsState(stateDomain.getDomainValue(position), doubleFactory.getNew(prob));
        //si l'état resultat à déja été enregistré
        if (rsStates.containsKey(position)) {

            RsState rsStateb = rsStates.get(position);
            //on ajoute la probabilité calculé pour une autre manière d'y acceder
            rsStateb.setProb(rsStateb.getProb().add(rsState.getProb()));

        } else {
            //sinon on se contente de l'ajouter dans la map
            rsStates.put(position, rsState);
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
}
