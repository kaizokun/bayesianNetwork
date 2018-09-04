package decision;

import environment.*;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MDPsimpleMap implements MDP<Position, Cardinal> {

    private SimpleMap simpleMap;

    private Map<State, List<Action>> stateActions = new Hashtable<>();

    private Map<State, Map<Action, List<Transition>>> stateTransitions = new Hashtable<>();

    public MDPsimpleMap(SimpleMap simpleMap) {

        this.simpleMap = simpleMap;
    }

    @Override
    public List<? extends State> getStates() {

        return simpleMap.getStates();
    }

    @Override
    public List<Action> getActions(Position state) {

        List<Action> actions = stateActions.get(state);

        if (actions == null) {

            actions = this.simpleMap.getActions(state);

            stateActions.put(state, actions);
        }

        return actions;
    }

    @Override
    public List<Transition> getTransitions(Position fromState, Cardinal action) {

        List<Transition> transitions;

        Map<Action, List<Transition>> actionsStates = this.stateTransitions.get(fromState);
        //si aucune transition n'est connu pour l'état ou aucune transition pour cette action precise
        if (actionsStates == null || actionsStates.get(action) == null) {

            //permet de savoir si une transition vers une position est déja enregistré pour augpmenter sa probabilité
            Map<Position, Transition> transitionsMap = new Hashtable<>();
            //action de base toujours valide car les actions dependent des positions atteignables ici immuables
            Position rsState = fromState.move(action);
            //action tout droit dans une direction vers une position atteignable
            transitionsMap.put(rsState, new Transition(0.8, rsState));

            //action droite ou direction suivante dans le sens des aiguilles d'une montre
            //repart de nord pour west
            int idDirection = (action.ordinal() + 1) % Cardinal.values().length;

            Cardinal relativeRight = Cardinal.getClockOrderValues()[idDirection];

            rsState = fromState.move(relativeRight);
            //si la position n'est pas atteignable on reste sur place
            if (!simpleMap.isPositionReachable(rsState)) {

                rsState = fromState;
            }

            transitionsMap.put(rsState, new Transition(0.1, rsState));

            //action gauche relativement à la direction de l'action
            //si on se deplace à gauche du nord soit à l'ouest
            idDirection = action.ordinal() - 1 < 0 ? Cardinal.values().length - 1 : action.ordinal() - 1;

            Cardinal relativeLeft = Cardinal.getClockOrderValues()[idDirection];

            rsState = fromState.move(relativeLeft);
            //si la position n'est pas atteignable on reste sur place
            if (!simpleMap.isPositionReachable(rsState)) {

                rsState = fromState;
            }
            //si la position a déja été ajoutée, cas ou le déplacement à droite et à gauche n'est pas possible
            //(tout droit fonctionne toujours ici)
            //on augmente la probabilité pour la position résultante
            if (transitionsMap.containsKey(rsState)) {

                transitionsMap.get(rsState).increaseProbability(0.1);

            }//sinon on se contente d'ajouter la transition
            else {

                transitionsMap.put(rsState, new Transition(0.1, rsState));
            }

            transitions = new LinkedList<>(transitionsMap.values());

            Map<Action, List<Transition>> actionStates = new Hashtable<>();

            actionStates.put(action, transitions);

            this.stateTransitions.put(fromState, actionStates);

        }else{

             transitions =  actionsStates.get(action);
        }

        return transitions;
    }

    @Override
    public Double getReward(Position state) {

        if (state.equals(simpleMap.getGoodExit())) {

            return 1.0;
        }

        if (state.equals(simpleMap.getBadExit())) {

            return -1.0;
        }

        return -0.04;
    }
}
