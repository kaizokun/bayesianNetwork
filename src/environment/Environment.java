package environment;

import java.util.List;
import java.util.Set;

public interface Environment<StateType extends State> {

    List<? extends Action> getActions(StateType state);

    Set<Position> getNotFinalStates();
}
