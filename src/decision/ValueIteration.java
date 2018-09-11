package decision;

import environment.Action;
import environment.State;
import environment.Transition;

import java.util.Hashtable;
import java.util.Map;

public class ValueIteration {

    private static int totalIterations;

    public static Map<State, Double> getUtility(MDP mdp, double error) {

        totalIterations = 0;

        Map<State, Double> currentU, previousU = new Hashtable<>();

        double maxUpdate;

        double limit = error * (1.0 - mdp.getDiscount()) / mdp.getDiscount();

        currentU = mdp.getInitialUtilityVector();

        do {

            maxUpdate = Double.NEGATIVE_INFINITY;
            //copie les valeurs d'utilité courant dans le vecteur d'utilités precedentes
            for (State state : mdp.getNotFinalStates()) {

                previousU.put(state, currentU.get(state));
            }

            //plus recopier les utilités des états finaux qui ne doivent pas êtres modifiés !
            for (State state : mdp.getFinalStates()) {

                previousU.put(state, currentU.get(state));
            }

            //pour chaque états non finaux
            for (State state : mdp.getNotFinalStates()) {

                MDPsimpleMap.MaxAction maxAction = mdp.getMaxAction(state, previousU);

                //mise a jour de l'utilité courante
                currentU.put(state, mdp.getReward(state) + (mdp.getDiscount() * maxAction.getUtility()));

                double currentUpdate = Math.abs(currentU.get(state) - previousU.get(state));

                if (currentUpdate > maxUpdate) {

                    maxUpdate = currentUpdate;
                }
            }

            totalIterations++;

        } while (maxUpdate >= limit);

        return previousU;
    }

    public static int getTotalIterations() {
        return totalIterations;
    }
}
