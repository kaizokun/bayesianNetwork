package decision;

import environment.Action;
import environment.State;

public interface Politic {

    Action getAction(State state);

    void setAction(Action action, State state);

    String toString();
}
