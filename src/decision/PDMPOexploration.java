package decision;

import domain.Domain;
import domain.data.AbstractDouble;
import environment.Percept;
import math.Distribution;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class PDMPOexploration {

    /*
     * DynamicBayesianNetwork network : nouveau reseau bayesien ou dumoins réinitialisé à zero pour exploration
     * Distribution forward : état de croyance courant sur les états dans le RBD "réel"
     * */

    public List<Variable> getBestAction(DynamicBayesianNetwork network, PDMPO pdmpo, Distribution forward) {

        return getBestAction(network, pdmpo, forward, 0);
    }

    private List<Variable> getBestAction(DynamicBayesianNetwork network, PDMPO pdmpo, Distribution forward, int time) {

        //on etend le network pour le temps time si necessaire
        if (network.getTime() <= time) {

            network.extend();

            System.out.println(network);
        }

        //distribution initiale
        Set<Domain.DomainValue> actions = pdmpo.getActionsFromState(forward);

        //pour chaque action ou combinaison d'actions possible depuis l'état de croyance courant
        //pour une combinaison d'action ou aura un object DomainValue composite
        for (Domain.DomainValue action : actions) {

            // System.out.println();
            // System.out.println("----------- ACTION : " + action);
            //crée une liste de percepts possible après avoir appliqué l'action aux états probables
            Map<Domain.DomainValue, AbstractDouble> perceptsMap = getPerceptsProb(forward, pdmpo, action);

            for (Map.Entry<Domain.DomainValue, AbstractDouble> entry : perceptsMap.entrySet()) {

                AbstractDouble perceptProb = entry.getValue();

                //recupere la variable (ou megavariable) action et percept
                Variable actionVar = pdmpo.getActionVar();

                Variable perceptVar = pdmpo.getPerceptVar();
                //initialise la valeur, si DomainValue composite les variables composant
                //la megavariable seront initialisées
                actionVar.setDomainValue(action);

                perceptVar.setDomainValue(entry.getKey());
                //initialise l'action pour le temps courant
                network.initVar(time, actionVar);
                //initialise le precept pour le temps suivant pour lequel calculer la distribution d'état
                network.initVar(time + 1, perceptVar);

                System.out.println("ACTION : "+action+" - PERCEPT : "+entry.getKey()+", "+entry.getValue());

                System.out.println(network);

                Distribution nextForward = network.forward(pdmpo.getStates(), pdmpo.getActions(), time + 1, forward);

                System.out.println(nextForward);

            }
        }

        return null;
    }

    private Map<Domain.DomainValue, AbstractDouble> getPerceptsProb(Distribution forward, PDMPO pdmpo, Domain.DomainValue action) {

        Map<Domain.DomainValue, AbstractDouble> perceptsMap = new Hashtable<>();

        //pour chaque état
        for (Domain.DomainValue state : forward.getRowValues()) {

            AbstractDouble stateProb = forward.get(state);
            //si cet etat à une probabilité superieur à zero
            if (stateProb.getDoubleValue().compareTo(0.0) > 0) {

                //  System.out.println();
                //  System.out.println("--> ETAT DE DEPART : " + state);
                //on applique l'action à un état de l'état de croyance complet
                //et on obtient les resultants avec leur probabilités si action non deterministe
                for (PDMPO.RsState rsState : pdmpo.getResultStates(state, action)) {

                    // System.out.println("<-- ETAT D' ARRIVEE : " + rsState.getState() + " - PROB : " + rsState.getProb());

                    //chaque etat resultat fourni un percept
                    //la probabilité de ce percept et la probabilité de l'état precedent
                    //multiplié par la probabilite de l'état resultant de l'action
                    //si plusieurs etats resultant fournissent le même percepts leur
                    //probabilités sont additionés

                    Domain.DomainValue percept = pdmpo.getPerceptFromState(rsState.getState());

                    //System.out.println("(0) PERCEPT " + percept);

                    AbstractDouble rsStatesProb = stateProb.multiply(rsState.getProb());

                    //System.out.println("COMPUTE : "+stateProb+" * "+rsState.getProb());

                    if (perceptsMap.containsKey(percept)) {

                        perceptsMap.put(percept, perceptsMap.get(percept).add(rsStatesProb));

                    } else {

                        perceptsMap.put(percept, rsStatesProb);
                    }

                    //System.out.println("TOTAL PROB PERCEPT " + perceptsMap.get(percept));
                }
            }
        }

        /*
            for (Map.Entry entry : perceptsMap.entrySet()) {

                System.out.println(entry.getKey() + " : " + entry.getValue());
            }
        */

        return perceptsMap;
    }

}
