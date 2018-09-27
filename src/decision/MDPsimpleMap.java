package decision;

import environment.*;

import java.util.*;

public class MDPsimpleMap implements MDP {

    private static final double GOAL_UTILITY = 1, FAIL_UTILITY = -1, STATES_REWARD = -0.04;

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

        for (Position state : this.simpleMap.getNotFinalStates()) {

            List<Cardinal> actions = this.simpleMap.getActions(state);

            stateActions.put(state, actions);

            for (Cardinal action : actions) {

                this.initTransition(state, action);
            }
        }
    }

    private void initTransition(Position fromState, Cardinal action) {

        //permet de savoir si une transition vers une position est déja enregistré pour augmenter sa probabilité
        Map<Position, Transition> transitionsMap = new Hashtable<>();
        //action de base toujours valide car les actions dependent des positions atteignables ici immuables
        Position rsState = fromState.move(action);
        //action tout droit dans une direction vers une position atteignable
        transitionsMap.put(rsState, new Transition(0.8, rsState, action));

        //action droite ou direction suivante dans le sens des aiguilles d'une montre
        //repart de nord pour west
        //int idDirection = (action.ordinalClock() + 1) % Cardinal.values().length;

        //Cardinal relativeRight = Cardinal.getClockOrderValues()[idDirection];

        //rsState = fromState.move(relativeRight);

        Cardinal relativeRight = action.getRelativeRight();

        rsState = fromState.move(relativeRight);

        //si la position n'est pas atteignable on reste sur place
        if (!simpleMap.isPositionReachable(rsState)) {

            rsState = fromState;
        }

        transitionsMap.put(rsState, new Transition(0.1, rsState, relativeRight));

        //action gauche relativement à la direction de l'action
        //si on se deplace à gauche du nord soit à l'ouest
        //idDirection = action.ordinalClock() - 1 < 0 ? Cardinal.values().length - 1 : action.ordinalClock() - 1;

        //Cardinal relativeLeft = Cardinal.getClockOrderValues()[idDirection];

        Cardinal relativeLeft = action.getRelativeLeft();

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
    public Map<State, Double> getInitialUtilityVector() {

        Map<State, Double> utility = new Hashtable();

        for (State state : this.simpleMap.getNotFinalStates()) {

            utility.put(state, 0.0);
        }

        utility.put(this.simpleMap.getGoodExit(), GOAL_UTILITY);

        utility.put(this.simpleMap.getBadExit(), FAIL_UTILITY);

        return utility;
    }

    @Override
    public Set<? extends State> getNotFinalStates() {

        return simpleMap.getNotFinalStates();
    }

    @Override
    public Set<? extends State> getFinalStates() {
        return this.simpleMap.getFinalStates();
    }

    @Override
    public List<? extends Action> getActions(State state) {

        return stateActions.get(state);
    }

    @Override
    public List<Transition> getTransitions(State fromState, Action action) {

        return stateTransitions.get(fromState).get(action);
    }

    @Override
    public Double getReward(State state) {

        if (state.equals(simpleMap.getGoodExit())) {

            return GOAL_UTILITY;
        }

        if (state.equals(simpleMap.getBadExit())) {

            return FAIL_UTILITY;
        }

        return STATES_REWARD;
    }

    @Override
    public Politic getRdmPolitic() {

        Map<State, Action> politic = new Hashtable<>();

        //pour chaque position non finale
        for (Position position : this.simpleMap.getNotFinalStates()) {

            List<Cardinal> actions = this.stateActions.get(position);

            int totalActions = actions.size();

            int rdmIAction = new Random().nextInt(totalActions);

            politic.put(position, actions.get(rdmIAction));
        }

        return new Politic() {
            @Override
            public Action getAction(State state) {

                return politic.get(state);
            }

            @Override
            public void setAction(Action action, State state) {
                politic.put(state, action);
            }

            @Override
            public String toString() {
                return politic.toString();
            }
        };
    }

    @Override
    public MaxAction getMaxAction(State state, Map<State, Double> utility) {

        //calcule l'utilité de l'action maximum
        Action maxAction = null;

        Double uActionMax = Double.NEGATIVE_INFINITY;
        //pour chaque action
        for (Action action : getActions(state)) {

            //calcule la somme des utilités des états resulnat de l'action pondérée par leur probabilité
            Double sumAction = getActionUtility(state, action, utility);

            //sauvegarde la valeur et l'action max
            if (sumAction > uActionMax) {

                maxAction = action;

                uActionMax = sumAction;
            }
        }

        return new MaxAction(maxAction, uActionMax);
    }

    @Override
    public Double getActionUtility(State state, Action action, Map<State, Double> utility) {

        Double sumAction = 0.0;
        //calcule la somme des utilités des états resulnat de l'action pondérée par leur probabilité
        for (Transition transition : getTransitions(state, action)) {

            sumAction += transition.getProbability() * utility.get(transition.getRsState());
        }

        return sumAction;
    }

    @Override
    public Double getDiscount() {
        return discount;
    }

    class MaxAction {

        private Action action;

        private double utility;

        public MaxAction(Action maxAction, double utility) {
            this.action = maxAction;
            this.utility = utility;
        }

        public Action getAction() {
            return action;
        }

        public void setAction(Action action) {
            this.action = action;
        }

        public double getUtility() {
            return utility;
        }

        public void setUtility(double utility) {
            this.utility = utility;
        }
    }

}
