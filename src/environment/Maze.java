package environment;

import agent.MazeRobot;
import java.util.*;

public class Maze {

    protected Set<Position> walls = new HashSet<>();

    protected MazeRobot robot;

    protected Position robotPosition;

    protected int limitX, limitY;

    protected Map<Position, Integer> totalAdjacent = new Hashtable<>();

    protected Map<Position, Set<Cardinal>> percepts = new Hashtable<>();

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

        this.limitX = limitX;

        this.limitY = limitY;

        this.robotPosition = robotPosition;

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

    public MazeRobot getRobot() {
        return robot;
    }

    public void setRobot(MazeRobot robot) {
        this.robot = robot;
    }

    public boolean isIn(Position pos) {

        return pos.y >= 0 && pos.y < limitY && pos.x >= 0 && pos.x < limitX;
    }

    public Set<Cardinal> getPercept() {

        return getPercept(this.robotPosition);
    }

    public Set<Cardinal> getPercept(Position position) {

        if(this.percepts.containsKey(position)){

            return this.percepts.get(position);
        }

        Set<Cardinal> percepts = new LinkedHashSet<>();

        //pour chaque case adjacente, direction N S E W, contenant un potentiel obstacle
        for(Cardinal cardinal : Cardinal.values()){
            //cree une position adjacente
            Position closePos = position.move(cardinal);
            //si la position est hors du labyrinthe ou est un mur,
            //il y a un obstacle dans cette direction
            if( !isIn(closePos) || walls.contains(closePos)){

                percepts.add(cardinal);
            }
        }

        this.percepts.put(position, percepts);

        return percepts;
    }

    public boolean matchPercept(Position position, List<Cardinal> percept) {

        Set<Cardinal> posPercept = getPercept(position);

        if( percept.size() != posPercept.size()){

            return false;
        }
/*
        //l'odre des cardinalités est identique à celui declaré dans l'enum
        //pour les deux ensembles à comparer
        Iterator<Cardinal> limits = posPercept.iterator();

        for(Cardinal direction : percept){

            if(!direction.equals(limits.next())){

                return false;
            }
        }
        */

        //autre manière un peu moins efficace
        for(Cardinal direction : percept){

            if(!posPercept.contains(direction)){

                return false;
            }
        }

        return true;
    }
}
