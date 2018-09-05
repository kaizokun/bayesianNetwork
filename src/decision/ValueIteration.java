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

        int i = 0;

        do {

            System.out.println("ITERATION "+i);

            maxUpdate = Double.MIN_VALUE;
            //copie les valeurs d'utilité courant dans le vecteur d'utilités precedentes
            for (State state : mdp.getStates()) {

                previousU.put(state, currentU.get(state));
            }

            System.out.println("PREVIOUS "+previousU);

            System.out.println("CURRENT "+currentU);

            //pour chaque états
            for (State state : mdp.getStates()) {

                System.out.println("STATE : "+state);

                //utilité de l'action maximum
                double uActionMax = -0.00001;
                //pour chaque action possible à partir de l'état courant
                for (Action action : mdp.getActions(state)) {
                    //somme des utilités des états resultant de l'action non deterministe
                    //pondérées par leur probabilités
                    double uRsStateSum = 0.0;

                    for (Transition transition : mdp.getTransitions(state, action)) {

                        System.out.println(transition.getProbability()+" * "+previousU.get(transition.getRsState()));

                        uRsStateSum += transition.getProbability() * previousU.get(transition.getRsState());
                    }

                    System.out.println("SUM "+uRsStateSum);
                    //calcul du maximum

                    System.out.println("MAX : "+uActionMax+" <> "+uRsStateSum+" "+(uRsStateSum > uActionMax));

                    if(uRsStateSum > uActionMax){

                        uActionMax = uRsStateSum;
                    }

                    System.out.println("MAX =  "+uActionMax);

                }
                //mise a jour de l'utilité courante
                currentU.put(state, mdp.getReward(state) + (mdp.getDiscount() * uActionMax));

                System.out.println("CURRENT UPDATE "+currentU);

                System.out.println(Math.abs(currentU.get(state) - previousU.get(state))+" <> "+limit);

                double currentUpdate = Math.abs(currentU.get(state) - previousU.get(state));

                if(currentUpdate > maxUpdate){

                    maxUpdate = currentUpdate;
                }

            }

            i ++;

        } while (maxUpdate >= limit);

        return previousU;
    }

}
