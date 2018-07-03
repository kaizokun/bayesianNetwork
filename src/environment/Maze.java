package environment;

import java.util.*;

public class Maze {

    protected Set<Position> walls = new HashSet<>();

    protected Position robotPosition;

    protected int limitX, limitY;

    protected Map<Position, Integer> totalAdjacent = new Hashtable<>();

    protected String[] maze;

    public Maze(int limitX, int limitY, String[] maze, Position robotPosition) {

        for (int rowStr = limitY - 1, row = 0; rowStr >= 0; rowStr--, row++) {

            String mazeRow = maze[rowStr];

            for (int col = 0; col < mazeRow.length(); col++) {

                if (mazeRow.charAt(col) == '#') {

                    walls.add(new Position(row, col));
                }
            }
        }

        this.robotPosition = robotPosition;

        this.limitX = limitX;

        this.limitY = limitY;

        this.maze = maze;
    }

    public int countReachablePositions(){

        return ( this.limitY * this.limitX ) - this.walls.size();
    }

    //get percepts

    public List<Position> getReachablePositions() {

        List<Position> positions = new LinkedList<>();

        for (int y = 0; y < limitY; y++) {

            for (int x = 0; x < limitX; x++) {

                if (!walls.contains(new Position(y, x))) {

                    positions.add(new Position(y, x));
                }
            }
        }

        return positions;
    }

    public boolean isWall(Position position) {

        return this.walls.contains(position);
    }

    public int totalAdjacent(Position position) {

        if (totalAdjacent.containsKey(position)) {

            return totalAdjacent.get(position);
        }

        int total = 0;
        //pour chaque position adjacente N S E W
        for (Cardinal cardinal : Cardinal.values()) {
            //copie et deplace la position
            Position adjacent = position.move(cardinal);
            //si dans les clous et pas un mur
            if (isIn(adjacent) && !isWall(adjacent)) {
                //on compte une case adjacente de plus
                total++;
            }
        }

        totalAdjacent.put(position, total);

        return total;
    }

    public int getLimitX() {
        return limitX;
    }

    public void setLimitX(int limitX) {
        this.limitX = limitX;
    }

    public int getLimitY() {
        return limitY;
    }

    public void setLimitY(int limitY) {
        this.limitY = limitY;
    }

    public boolean isIn(Position pos) {

        return pos.y >= 0 && pos.y < limitY && pos.x >= 0 && pos.x < limitX;
    }
}
