package test.decision.NotDeterministic;

import decision.*;
import environment.Cardinal;
import environment.Position;
import environment.SimpleMap;
import environment.State;
import org.junit.Test;

import java.util.Map;

public class SimpleMapTest {


    @Test
    public void simpleMapConstructTest() {

        String map[] = new String[]{"    ",
                " #  ",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map, new Position(3, 4), new Position(2, 4));

        System.out.println("STATES : " + simpleMap.getNotFinalStates());

        System.out.println("WALLS : " + simpleMap.getWalls());
    }

    @Test
    public void simpleMapActionsTest() {

        String map[] = new String[]{"    ",
                " #  ",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map, new Position(3, 4), new Position(2, 4));

        System.out.println(" (1,1) " + simpleMap.getActions(new Position(1, 1)));

        System.out.println(" (1,2) " + simpleMap.getActions(new Position(1, 2)));

        System.out.println(" (2,3) " + simpleMap.getActions(new Position(2, 3)));
    }

    @Test
    public void simpleMapTransitionsTest() {

        String map[] = new String[]{"    ",
                " #  ",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map, new Position(3, 4), new Position(2, 4));

        MDPsimpleMap mdPsimpleMap = new MDPsimpleMap(simpleMap, null);

        System.out.println(" (1,1) -> NORTH " + mdPsimpleMap.getTransitions(new Position(1, 1), Cardinal.NORTH));

        System.out.println(" (1,1) -> EAST " + mdPsimpleMap.getTransitions(new Position(1, 1), Cardinal.EAST));

        System.out.println(" (1,2) -> EAST " + mdPsimpleMap.getTransitions(new Position(1, 2), Cardinal.EAST));

        System.out.println(" (1,2) -> WEST " + mdPsimpleMap.getTransitions(new Position(1, 2), Cardinal.WEST));

        System.out.println(" (2,3) -> EAST " + mdPsimpleMap.getTransitions(new Position(2, 3), Cardinal.EAST));
    }

    @Test
    public void simpleMapValueIterationTest() {

        double discount = 0.99;

        double error = 0.0001;

        String map[] = new String[]{"    ",
                " #  ",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map, new Position(3, 4), new Position(2, 4));

        MDP mdPsimpleMap = new MDPsimpleMap(simpleMap, discount);

        Map<State, Double> utility = ValueIteration.getUtility(mdPsimpleMap, error);

        System.out.println("DISCOUNT : "+discount+", ERROR : "+error);

        for (Map.Entry<State, Double> entry : utility.entrySet()) {

            System.out.println("POSITION : "+entry.getKey()+", Utilité : "+entry.getValue());
        }

        System.out.println("TOTAL ITERATION : "+ValueIteration.getTotalIterations());

    }

    @Test
    public void simpleMapRdmPoliticTest(){

        String map[] = new String[]{"    ",
                " #  ",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map, new Position(3, 4), new Position(2, 4));

        MDP mdPsimpleMap = new MDPsimpleMap(simpleMap, 0.0);

        Politic politic = mdPsimpleMap.getRdmPolitic();

        for(State state : simpleMap.getNotFinalStates()){

            System.out.println("STATE "+state+" - Action "+politic.getAction(state));
        }

    }

    @Test
    public void simpleMapPoliticIterationTest() {

        double discount = 0.9999;

        String map[] = new String[]{"    ",
                " #  ",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map, new Position(3, 4), new Position(2, 4));

        MDP mdPsimpleMap = new MDPsimpleMap(simpleMap, discount);

        Politic politic = PoliticIteration.getBestPoliticy(mdPsimpleMap);

        System.out.println(politic);

    }

}
