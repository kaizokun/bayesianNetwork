package decision;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import environment.Cardinal;
import environment.Position;
import environment.SimpleMap;
import math.Distribution;
import network.Variable;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.POSITION;

public class PDMPOSimpleMap implements PDMPO{

    protected SimpleMap simpleMap;

    protected IDomain actionDomain, perceptDomain, stateDomain;

    public PDMPOSimpleMap(SimpleMap simpleMap) {

        this.simpleMap = simpleMap;

        this.actionDomain = DomainFactory.getCardinalDomain();

        this.perceptDomain = DomainFactory.getMazeWallCaptorDomain();
        //normalement l'ordre des positions est identiques que dans le RDB
        this.stateDomain = DomainFactory.getPositionsDomain(simpleMap.getAllStates());
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
        for(Domain.DomainValue value : forward.getRowValues()){
            //pour les états probables > 0
            if(forward.get(value).getDoubleValue().compareTo(0.0) > 0){
                //recupere l'object position stocké dans la valeur de domaine
                Position position = (Position) value.getValue();
                //pour chaque direction alentours
                for(Cardinal cardinal : Cardinal.values()){

                    Position nextPosition = position.move(cardinal);

                    if(simpleMap.isPositionReachable(nextPosition)){

                        actions.add(actionDomain.getDomainValue(cardinal));
                    }
                }
            }
        }

        return actions;

    }
}
