package environment;

import java.util.List;

public interface Environment<StateType extends State> {

    List<? extends Action> getActions(StateType state);

}
