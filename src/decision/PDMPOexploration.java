package decision;

import domain.Domain;
import domain.data.AbstractDouble;
import math.Distribution;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.asList;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.POSITION;

public class PDMPOexploration {

    /*
     * DynamicBayesianNetwork network : nouveau reseau bayesien ou dumoins réinitialisé à zero pour exploration
     * Distribution forward : état de croyance courant sur les états dans le RBD "réel"
     * */
    public List<Variable> getBestAction(DynamicBayesianNetwork network, PDMPO pdmpo, Distribution forward) {

        //distribution initiale

        Set<Domain.DomainValue> actions = pdmpo.getActionsFromState(forward);

        //pour chaque action possible depuis l'état de croyance courant
        for(Domain.DomainValue action : actions) {

            System.out.println("ACTION : " + action);
            //crée une liste de percepts possible après avoir appliqué l'action aux états probables
            Map<Domain.DomainValue, AbstractDouble> perceptsMap = new Hashtable<>();
            //pour chaque état
            for (Domain.DomainValue state : forward.getRowValues()) {

                AbstractDouble stateProb = forward.get(state);
                //si cet etat à une probabilité superieur à zero
                if(stateProb.getDoubleValue().compareTo(0.0) > 0) {

                    //on applique l'action à un état de l'état de croyance complet
                    //et on obtient les resultants avec leur probabilités si action non deterministe
                    for (PDMPO.RsState rsState : pdmpo.getResultStates(state, action)) {

                        //chaque etat resultat fourni un percept
                        //la probabilité de ce percept et la probabilité de l'état precedent
                        //multiplié par la probabilite de l'état resultant de l'action
                        //si plusieurs etats resultant fournissent le même percepts leur
                        //probabilités sont additionés

                        Domain.DomainValue percept = pdmpo.getPerceptFromState(rsState.getState());

                        AbstractDouble rsStatesProb = stateProb.multiply(rsState.getProb());

                        if(perceptsMap.containsKey(percept)){

                            perceptsMap.put(percept, perceptsMap.get(percept).add(rsStatesProb));

                        }else{

                            perceptsMap.put(percept,rsStatesProb);
                        }
                    }
                }
            }

            System.out.println("PERCEPTS "+perceptsMap);
        }

        return null;
    }

}
