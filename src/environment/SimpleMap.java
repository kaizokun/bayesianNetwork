package environment;

import java.util.LinkedList;
import java.util.List;

public class SimpleMap implements Environment<Position> {

    private static final char WALL = '#';

    private String[] map;

    private Position goodExit, badExit;

    private List<Position> states = new LinkedList<>();

    private int xLimit, yLimit;


    public SimpleMap(String[] map, Position goodExit, Position badExit) {

        this.map = map;

        this.goodExit = goodExit;

        this.badExit = badExit;

        this.xLimit = map[0].length();

        this.yLimit = map.length;

        for (int iRow = 0; iRow < map.length; iRow++) {

            String row = map[iRow];

            for (int iCol = 0; iCol < row.length(); iCol++) {

                if (row.charAt(iCol) != WALL) {

                    states.add(new Position(yLimit - iRow, iCol + 1));
                }
            }
        }
    }

    /*
    * 3
    * 2
    * 1
    *   1 2 3 4
    * */

    public boolean isPositionIn(Position position) {

        return position.getX() > 0 && position.getY() > 0 && position.getX() <= xLimit && position.getY() <= yLimit;
    }

    @Override
    public List<Action> getActions(Position state) {

        List<Action> actions = new LinkedList<>();

        for(Cardinal direction : Cardinal.values()){

            Position adjPos = state.move(direction);

            if(isPositionIn(adjPos)){

                actions.add(direction);
            }
        }

        return actions;
    }

    public List<Position> getStates() {

        return states;
    }

    public Position getGoodExit() {
        return goodExit;
    }

    public Position getBadExit() {
        return badExit;
    }
}
