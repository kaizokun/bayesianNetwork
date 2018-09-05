package decision;

import environment.Action;
import environment.State;
import environment.Transition;

import java.util.List;

public interface MDP<StateType extends State, ActionType extends Action> {

    List<? extends State> getStates();

    List<? extends Action> getActions(StateType state);

    List<Transition> getTransitions(StateType fromState, ActionType action);

    Double getReward(StateType state);

    Double getDiscount();
}
