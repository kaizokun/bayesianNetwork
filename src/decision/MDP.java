package decision;

import environment.Action;
import environment.State;
import environment.Transition;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface MDP<StateType extends State, ActionType extends Action> {

    Set<? extends State> getNotFinalStates();

    Set<? extends State> getFinalStates();

    List<? extends Action> getActions(StateType state);

    List<Transition> getTransitions(StateType fromState, ActionType action);

    Double getReward(StateType state);

    Double getDiscount();

    Map<State,Double> getInitialUtilityVector();
}
