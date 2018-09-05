package decision;

import environment.Action;
import environment.State;
import environment.Transition;

import java.util.Hashtable;
import java.util.Map;

public class ValueIteration {

    public static Map<State, Double> getUtility(MDP<State, Action> mdp, double error) {

        Map<State, Double> currentU = new Hashtable<>(), previousU = new Hashtable<>();

        double maxUpdate;

        double limit = error * (1.0 - mdp.getDiscount()) / mdp.getDiscount();

        for (State state : mdp.getStates()) {

            currentU.put(state, 0.0);

            // previousU.put(state, 0.0);
        }

        do {

            maxUpdate = 0.0;
            //copie les valeurs d'utilité courant dans le vecteur d'utilités precedentes
            for (State state : mdp.getStates()) {

                previousU.put(state, currentU.get(state));
            }
            //pour chaque états
            for (State state : mdp.getStates()) {
                //utilité de l'action maximum
                double uActionMax = 0.0;
                //pour chaque action possible à partir de l'état courant
                for (Action action : mdp.getActions(state)) {
                    //somme des utilités des états resultant de l'action non deterministe
                    //pondérées par leur probabilités
                    double uRsStateSum = 0.0;

                    for (Transition transition : mdp.getTransitions(state, action)) {

                        uRsStateSum += transition.getProbability() * previousU.get(transition.getRsState());
                    }
                    //calcul du maximum
                    uActionMax = Math.max(uRsStateSum, uActionMax);

                }
                //mise a jour de l'utilité courante
                currentU.put(state, mdp.getReward(state) + (mdp.getDiscount() * uActionMax));

                maxUpdate = Math.max(maxUpdate, currentU.get(state) - previousU.get(state));
            }

        } while (maxUpdate < limit);

        return previousU;
    }

}
