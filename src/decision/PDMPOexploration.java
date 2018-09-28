package decision;

import domain.Domain;
import math.Distribution;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.POSITION;

public class PDMPOexploration {

    /*
    * DynamicBayesianNetwork network : nouveau reseau bayesien ou dumoins réinitialisé à zero pour exploration
    * Distribution forward : état de croyance courant sur les états dans le RBD "réel"
    * */
    public List<Variable> getBestAction(DynamicBayesianNetwork network, PDMPO pdmpo, Distribution forward){

        //distribution initiale

        Set<Domain.DomainValue> actions = pdmpo.getActionsFromState(forward);

        System.out.println("ACTIONS : "+actions);

        return null;
    }

}
