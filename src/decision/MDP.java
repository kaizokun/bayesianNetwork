package decision;

import environment.Action;
import environment.State;
import environment.Transition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MDP {

    Set<? extends State> getNotFinalStates();

    Set<? extends State> getFinalStates();

    List<? extends Action> getActions(State state);

    List<Transition> getTransitions(State fromState, Action action);

    Double getReward(State state);

    MDPsimpleMap.MaxAction getMaxAction(State state, Map<State, Double> utility);

    Double getActionUtility(State state, Action action, Map<State, Double> utility);

    Double getDiscount();

    Map<State, Double> getInitialUtilityVector();

    Politic getRdmPolitic();
}
