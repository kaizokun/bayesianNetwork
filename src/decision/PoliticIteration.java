package decision;

import environment.Action;
import environment.State;
import environment.Transition;

import java.util.Hashtable;
import java.util.Map;

public class PoliticIteration {

    public static Politic getBestPoliticy(MDP<State, Action> mdp) {

        Map<State, Double> utility = mdp.getInitialUtilityVector();

        Politic politic = mdp.getRdmPolitic();

        utility = evaluatePolitic(utility, politic, mdp);

        return politic;
    }


    private static Map<State, Double> evaluatePolitic(Map<State, Double> utility, Politic politic, MDP<State, Action> mdp) {

        Map<State, Double> nextUtility = new Hashtable<>();
        //recopie les utilités des états finaux dans le nouveau vecteur
        //si tenté qu'il faille en utiliser un nouveau, le pseudo code du livre
        //laisse penser qu'il faut utiliser le même vecteur mais dans ce cas on calcule les utilités
        //en fonction d'une politique à partir d'utilié en cour de modification ...
        for(State finalState : mdp.getFinalStates()){

            nextUtility.put(finalState, utility.get(finalState));
        }

        for (State state : mdp.getNotFinalStates()) {

            Double sum = 0.0;

            for(Transition transition : mdp.getTransitions(state, politic.getAction(state))){

                sum += transition.getProbability() * utility.get(transition.getRsState());
            }

            Double uValue = mdp.getReward(state) + mdp.getDiscount() * sum;

            nextUtility.put(state, uValue);
        }

        return nextUtility;
    }

}
