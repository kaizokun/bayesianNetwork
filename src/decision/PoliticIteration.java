package decision;

import environment.State;
import environment.Transition;

import java.util.Map;

public class PoliticIteration {

    public static int iterations;

    public static Politic getBestPoliticy(MDP mdp) {

        return getBestPoliticy(mdp, false, 0);
    }

    public static Politic getBestPoliticy(MDP mdp, int limit) {

        return getBestPoliticy(mdp, true, limit);
    }

    public static Politic getBestPoliticy(MDP mdp, boolean hasLimit, int limit) {

        //Vecteur d'utilité de base avec les utilité à zero pour les états non finaux,
        //pour l'état but -1 pour l'état echec
        Map<State, Double> utility = mdp.getInitialUtilityVector();
        //politique aléatoire associant une action aléatoire à un état
        Politic politic = mdp.getRdmPolitic();

        boolean changed;

        iterations = 0;

        int iteration = 0;

        do {
            //évalue l'utilité à partir de l'utilité courante et d'une politique fixe
            //simplification de l'équation de Bellman ou les actions sont figées
            //plutôt que de choisir une action maximum
            evaluatePolitic(utility, politic, mdp);

           // evaluatePolitic(utility, politic, mdp);

            changed = false;
            //pour chaque état final
            for (State state : mdp.getNotFinalStates()) {

                //calcule l'utilité de l'action maximum

                MDPsimpleMap.MaxAction max = mdp.getMaxAction(state, utility);

                Double uActionPolitic = 0.0;
                //calcul l'utilité espere de l'action de la politique courante
                for (Transition transition : mdp.getTransitions(state, politic.getAction(state))) {

                    uActionPolitic += transition.getProbability() * utility.get(transition.getRsState());
                }
                //compare l'action de la politique courante à celle offrant le maximum
                //et modifie la politique si elle peut être amélioré
                if (max.getUtility() > uActionPolitic) {

                    politic.setAction(max.getAction(), state);

                    changed = true;
                }
            }

            iteration++;

            //tant que lapolitique change && que : il n'y a pas de limite ou
            //qu'il y a une limite et elle n'a pas été franchi
        } while (changed && (!hasLimit || iteration < limit));

        iterations = iteration;

        return politic;
    }

    private static void evaluatePolitic(Map<State, Double> utility, Politic politic, MDP mdp) {

        //on calcule les nouvelles utilités à partir du vecteur en cour de modification
        //sinon les actions ne sont pas cohérents entres elles

        //pour chaque état non final
        for (State state : mdp.getNotFinalStates()) {

            Double sum = mdp.getActionUtility(state, politic.getAction(state), utility);
            //recompense de l'état plus l'escompte multipliée par la sommme
            Double uValue = mdp.getReward(state) + (mdp.getDiscount() * sum);
            //enregistre la nouvelle utilité
            utility.put(state, uValue);
        }
    }

}
