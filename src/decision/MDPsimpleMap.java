package decision;

import environment.*;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MDPsimpleMap implements MDP<Position, Cardinal> {

    private SimpleMap simpleMap;

    private Map<State, List<Cardinal>> stateActions = new Hashtable<>();

    private Map<State, Map<Cardinal, List<Transition>>> stateTransitions = new Hashtable<>();

    private Double discount;

    public MDPsimpleMap(SimpleMap simpleMap) {

        this(simpleMap, null);
    }

    public MDPsimpleMap(SimpleMap simpleMap, Double discount) {

        this.simpleMap = simpleMap;

        this.discount = discount;

        this.initActionsTransitions();
    }

    private void initActionsTransitions() {

        for (Position state : this.simpleMap.getStates()) {

            List<Cardinal> actions = this.simpleMap.getActions(state);

            stateActions.put(state, actions);

            for (Cardinal action : actions) {

                this.initTransition(state, action);
            }
        }
    }

    private void initTransition(Position fromState, Cardinal action) {

        //permet de savoir si une transition vers une position est déja enregistré pour augpmenter sa probabilité
        Map<Position, Transition> transitionsMap = new Hashtable<>();
        //action de base toujours valide car les actions dependent des positions atteignables ici immuables
        Position rsState = fromState.move(action);
        //action tout droit dans une direction vers une position atteignable
        transitionsMap.put(rsState, new Transition(0.8, rsState, action));

        //action droite ou direction suivante dans le sens des aiguilles d'une montre
        //repart de nord pour west
        int idDirection = (action.ordinalClock() + 1) % Cardinal.values().length;

        Cardinal relativeRight = Cardinal.getClockOrderValues()[idDirection];

        rsState = fromState.move(relativeRight);

        //si la position n'est pas atteignable on reste sur place
        if (!simpleMap.isPositionReachable(rsState)) {

            rsState = fromState;
        }

        transitionsMap.put(rsState, new Transition(0.1, rsState, relativeRight));

        //action gauche relativement à la direction de l'action
        //si on se deplace à gauche du nord soit à l'ouest
        idDirection = action.ordinalClock() - 1 < 0 ? Cardinal.values().length - 1 : action.ordinalClock() - 1;

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

            transitionsMap.put(rsState, new Transition(0.1, rsState, relativeLeft));
        }

        List<Transition> transitions = new LinkedList<>(transitionsMap.values());

        Map<Cardinal, List<Transition>> actionTransitionsMap = this.stateTransitions.get(fromState);

        if (actionTransitionsMap == null) {

            actionTransitionsMap = new Hashtable<>();

            this.stateTransitions.put(fromState, actionTransitionsMap);
        }

        actionTransitionsMap.put(action, transitions);
    }

    @Override
    public List<? extends State> getStates() {

        return simpleMap.getStates();
    }

    @Override
    public List<? extends Action> getActions(Position state) {

        return stateActions.get(state);
    }

    @Override
    public List<Transition> getTransitions(Position fromState, Cardinal action) {

        return stateTransitions.get(fromState).get(action);
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

    @Override
    public Double getDiscount() {
        return discount;
    }
}
