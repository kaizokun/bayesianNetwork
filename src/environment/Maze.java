package environment;

import agent.MazeRobot;
import agent.MazeRobot.PositionProb;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class Maze {

    protected Set<Position> walls = new HashSet<>();

    protected MazeRobot robot;

    protected Position robotPosition;

    protected int limitX, limitY;

    protected Map<Position, Integer> totalAdjacent = new Hashtable<>();

    protected Map<Position, Set<Cardinal>> percepts = new Hashtable<>();

    protected List<PositionProb> reachablePositions = new LinkedList<>();

    protected String[] maze;

    protected AbstractDoubleFactory doubleFactory;

    public Maze(String[] maze, Position robotPosition, AbstractDoubleFactory doubleFactory) {

        this.limitX = maze[0].length();

        this.limitY = maze.length;

        for (int rowStr = this.limitY - 1, row = 0; rowStr >= 0; rowStr--, row++) {

            String mazeRow = maze[rowStr];

            for (int col = 0; col < mazeRow.length(); col++) {

                if (mazeRow.charAt(col) == '#') {

                    walls.add(new Position(row, col));
                }
            }
        }

        this.robotPosition = robotPosition;

        this.maze = maze;

        this.doubleFactory = doubleFactory;
    }

    public int countReachablePositions() {

        // return (this.limitY * this.limitX) - this.walls.size();
        return this.getReachablePositions().size();
    }

    //get percepts

    public List<PositionProb> getReachablePositions() {

        if (reachablePositions.isEmpty()) {

            for (int y = 0; y < limitY; y++) {

                for (int x = 0; x < limitX; x++) {

                    if (!walls.contains(new Position(y, x))) {

                        reachablePositions.add(new PositionProb(new Position(y, x), null));
                    }
                }
            }

            double initProb = 1.0 / reachablePositions.size();

            for(PositionProb positionProb : reachablePositions){

                positionProb.setProb(doubleFactory.getNew(initProb));
            }
        }

        return reachablePositions;
    }

    public List<PositionProb> getNewReachablePosition(List<PositionProb> positionsProb) {

        Set<PositionProb> allPositions = new LinkedHashSet<>();

        allPositions.addAll(positionsProb);

        //pour chaque positions
        for (PositionProb position : positionsProb) {
            //chaque position alentours
            for (Cardinal direction : Cardinal.values()) {

                Position positionB = position.getPosition().move(direction);
                //si la position est atteignable
                if (isIn(positionB) && !isWall(positionB)) {

                    allPositions.add(new PositionProb(positionB, doubleFactory.getNew(0.0)));
                }
            }
        }

        return new ArrayList(allPositions);
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

    public boolean isWall(Position position) {

        return this.walls.contains(position);
    }


    public boolean isIn(Position pos) {

        return pos.y >= 0 && pos.y < limitY && pos.x >= 0 && pos.x < limitX;
    }

    public Set<Cardinal> getPercept() {

        return getPercept(this.robotPosition);
    }

    public void moveRobot(Cardinal cardinal) {

        this.robotPosition = this.robotPosition.move(cardinal);
    }

    public Position getRobotPosition() {

        return robotPosition;
    }

    public Set<Cardinal> getPercept(Position position) {

        if (this.percepts.containsKey(position)) {

            return this.percepts.get(position);
        }

        Set<Cardinal> percepts = new LinkedHashSet<>();

        //pour chaque case adjacente, direction N S E W, contenant un potentiel obstacle
        for (Cardinal cardinal : Cardinal.values()) {
            //cree une position adjacente
            Position closePos = position.move(cardinal);
            //si la position est hors du labyrinthe ou est un mur,
            //il y a un obstacle dans cette direction
            if (!isIn(closePos) || walls.contains(closePos)) {

                percepts.add(cardinal);
            }
        }

        this.percepts.put(position, percepts);

        return percepts;
    }

    public boolean matchPercept(Position position, List<Cardinal> percept) {

        Set<Cardinal> posPercept = getPercept(position);

        if (percept.size() != posPercept.size()) {

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
        for (Cardinal direction : percept) {

            if (!posPercept.contains(direction)) {

                return false;
            }
        }

        return true;
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

    public void setReachablePositions(List<PositionProb> reachablePositions) {

        this.reachablePositions = reachablePositions;
    }
}
