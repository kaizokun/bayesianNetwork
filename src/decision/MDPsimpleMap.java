package decision;

import environment.*;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MDPsimpleMap implements MDP<Position, Cardinal> {

    private SimpleMap simpleMap;

    private Map<State, List<Action>> stateActions = new Hashtable<>();

    public MDPsimpleMap(SimpleMap simpleMap) {

        this.simpleMap = simpleMap;
    }

    @Override
    public List<? extends State> getStates() {

        return simpleMap.getStates();
    }

    @Override
    public List<Action> getActions(Position state) {

        if (stateActions.containsKey(state)) {

            return stateActions.get(state);
        }

        List<Action> actions = this.simpleMap.getActions(state);

        for (Cardinal direction : Cardinal.values()) {

            Position adjPos = state.move(direction);

            if (simpleMap.isPositionIn(adjPos)) {

                actions.add(direction);
            }
        }

        stateActions.put(state, actions);

        return actions;
    }

    @Override
    public List<Transition> getTransitionProb(Position fromState, Cardinal action) {



        return null;
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
