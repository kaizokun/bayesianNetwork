package decision;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDoubleFactory;
import environment.Cardinal;
import environment.Position;
import environment.SimpleMap;
import math.Distribution;
import network.Variable;

import java.util.*;

import static java.util.Arrays.asList;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.POSITION;

public class PDMPOSimpleMap implements PDMPO {

    protected SimpleMap simpleMap;

    protected IDomain actionDomain, perceptDomain, stateDomain;

    protected AbstractDoubleFactory doubleFactory;

    public PDMPOSimpleMap(SimpleMap simpleMap, AbstractDoubleFactory doubleFactory) {

        this.simpleMap = simpleMap;

        this.actionDomain = DomainFactory.getCardinalDomain();

        this.perceptDomain = DomainFactory.getMazeWallCaptorDomain();
        //normalement l'ordre des positions est identiques que dans le RDB
        this.stateDomain = DomainFactory.getPositionsDomain(simpleMap.getAllStates());

        this.doubleFactory = doubleFactory;
    }

    @Override
    public List<Variable> getStates() {

        return asList(new Variable(POSITION));
    }

    @Override
    public List<Variable> getActions() {

        return asList(new Variable(MOVE));
    }

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
                for (Cardinal cardinal : Cardinal.values()) {

                    Position nextPosition = position.move(cardinal);

                    if (simpleMap.isPositionReachable(nextPosition)) {

                        actions.add(actionDomain.getDomainValue(cardinal));
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

        Cardinal direction = (Cardinal) action.getValue();

        addNewPosition(rsStates, position, direction, 0.8);

        addNewPosition(rsStates, position, direction.getRelativeRight(), 0.1);

        addNewPosition(rsStates, position, direction.getRelativeLeft(), 0.1);

        return rsStates.values();
    }

    private void addNewPosition(Map<Position, RsState> rsStates, Position position, Cardinal direction, double prob) {
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

        }else{
            //sinon on se contente de l'ajouter dans la map
            rsStates.put(position, rsState);
        }

    }

    @Override
    public Domain.DomainValue getPerceptFromState(Domain.DomainValue state) {
        //la classe simple map fourni le percept à partir de la map et de la position
        return perceptDomain.getDomainValue(this.simpleMap.getPercept((Position) state.getValue()));
    }
}
