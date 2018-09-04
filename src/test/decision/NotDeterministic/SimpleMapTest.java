package test.decision.NotDeterministic;

import decision.MDPsimpleMap;
import environment.Cardinal;
import environment.Position;
import environment.SimpleMap;
import org.junit.Test;

public class SimpleMapTest {


    @Test
    public void simpleMapConstructTest() {

        String map[] = new String[]{"    ",
                " #  ",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map, new Position(3, 4), new Position(2, 4));

        System.out.println("STATES : " + simpleMap.getStates());

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

        MDPsimpleMap mdPsimpleMap = new MDPsimpleMap(simpleMap);

        System.out.println(" (1,1) -> NORTH " + mdPsimpleMap.getTransitions(new Position(1, 1), Cardinal.NORTH));

        System.out.println(" (1,1) -> EAST " + mdPsimpleMap.getTransitions(new Position(1, 1), Cardinal.EAST));

        System.out.println(" (1,2) -> EAST " + mdPsimpleMap.getTransitions(new Position(1, 2), Cardinal.EAST));

        System.out.println(" (1,2) -> WEST " + mdPsimpleMap.getTransitions(new Position(1, 2), Cardinal.WEST));

        System.out.println(" (2,3) -> EAST " + mdPsimpleMap.getTransitions(new Position(2, 3), Cardinal.EAST));
    }

}
