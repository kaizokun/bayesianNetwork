package environment;

import java.util.List;

public interface Environment<StateType extends State> {

    List<Action> getActions(StateType state);

}
