package environment;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class SimpleMap implements Environment<Position> {

    private static final char WALL = '#';

    private String[] map;

    private Position goodExit, badExit;

    private List<Position> states = new LinkedList<>();

    private Set<Position> walls = new HashSet<>();

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

                }else{

                    walls.add(new Position(yLimit - iRow, iCol + 1));
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

    public boolean isPositionReachable(Position position) {

        //dans les limites de la map est pas un mur
        return position.getX() > 0 && position.getY() > 0 && position.getX() <= xLimit && position.getY() <= yLimit && !walls.contains(position);
    }

    @Override
    public List<Cardinal> getActions(Position state) {

        List<Cardinal> actions = new LinkedList<>();

        for(Cardinal direction : Cardinal.values()){

            Position adjPos = state.move(direction);

            if(isPositionReachable(adjPos)){

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


    public Set<Position> getWalls() {
        return walls;
    }
}
