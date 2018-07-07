package environment;

import agent.MazeRobot;
import agent.MazeRobot.PositionProb;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class Maze {

    protected MazeRobot robot;

    protected Position robotPosition;

    protected int limitX, limitY;

    protected Map<Position, Integer> totalAdjacent = new Hashtable<>();

    protected Map<Position, Percept> percepts = new Hashtable<>();

    protected String[] maze;

    private String[][] strMaze;

    private List<Position> resetPositions = new LinkedList<>();

    protected AbstractDoubleFactory doubleFactory;

    private static final char wall = '#';

    public Maze(String[] maze, Position robotPosition, AbstractDoubleFactory doubleFactory) {

        this.limitX = maze[0].length();

        this.limitY = maze.length;

        this.robotPosition = robotPosition;

        this.maze = maze;

        this.doubleFactory = doubleFactory;

        this.initStrMaze();
    }

    public List<PositionProb> getInitReachablePositions() {

        List<PositionProb> positions = new LinkedList<>();

        for (int y = 0; y < limitY; y++) {

            for (int x = 0; x < limitX; x++) {

                if (!isWall(new Position(y, x))) {

                    positions.add(new PositionProb(new Position(y, x), null));
                }
            }
        }

        double initProb = 1.0 / positions.size();

        for (PositionProb positionProb : positions) {

            positionProb.setProb(doubleFactory.getNew(initProb));
        }

        return positions;
    }

    public List<PositionProb> getNewReachablePosition(List<PositionProb> positionsProb) {

        Set<PositionProb> allPositions = new LinkedHashSet<>(positionsProb);

        //pour chaque positions
        for (PositionProb position : positionsProb) {
            //chaque position alentours

            for (Position nearbyPosition : position.getPosition().getNearbyPositions()) {

                if (isReachablePosition(nearbyPosition)) {

                    allPositions.add(new PositionProb(nearbyPosition, doubleFactory.getNew(0.001)));
                }
            }
        }

        return new ArrayList(allPositions);
    }

    private boolean isReachablePosition(Position position) {

        return isIn(position) && !isWall(position);
    }

    private boolean isWall(Position position) {

        return this.maze[this.limitY - 1 - position.y].charAt(position.x) == wall;
    }

    private boolean isIn(Position pos) {

        return pos.y >= 0 && pos.y < limitY && pos.x >= 0 && pos.x < limitX;
    }

    public int totalAdjacent(Position position) {

        if (totalAdjacent.containsKey(position)) {

            return totalAdjacent.get(position);
        }

        int total = 0;

        for (Position nearbyPosition : position.getNearbyPositions()) {

            if (this.isReachablePosition(nearbyPosition)) {
                //on compte une case adjacente de plus
                total++;
            }
        }

        totalAdjacent.put(position, total);

        return total;
    }

    public void moveRobot(Cardinal cardinal) {

        this.robotPosition = this.robotPosition.move(cardinal);
    }

    public Position getRobotPosition() {

        return robotPosition;
    }

    public Percept getPercept() {

        return getPercept(this.robotPosition);
    }

    public Percept getPercept(Position position) {

        if (this.percepts.containsKey(position)) {

            return this.percepts.get(position);
        }

        PerceptWall perceptWall = new PerceptWall();

        //pour chaque case adjacente, direction N S E W, contenant un potentiel obstacle
        for (Cardinal cardinal : Cardinal.values()) {
            //cree une position adjacente
            Position closePos = position.move(cardinal);
            //si la position est hors du labyrinthe ou est un mur,
            //il y a un obstacle dans cette direction

            if (!isReachablePosition(closePos)) {

                perceptWall.addWallDirection(cardinal);
            }
        }

        this.percepts.put(position, perceptWall);

        return perceptWall;
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

    /*----------------VIEW----------------*/

    private void initStrMaze() {

        this.strMaze = new String[this.maze.length + 1][this.maze[0].length() + 1];

        for (int row = 0; row < strMaze.length - 1; row++) {

            for (int col = 0; col < strMaze[0].length; col++) {

                if (col == 0) {

                    this.strMaze[row][col] = new String("[" + (this.maze.length - 1 - row) + "]");

                } else {

                    if (maze[row].charAt(col - 1) == '#') {

                        this.strMaze[row][col] = new String("[##]");

                    } else {

                        this.strMaze[row][col] = new String("[  ]");
                    }
                }
            }
        }

        this.strMaze[this.strMaze.length - 1][0] = new String("[ ]");

        for (int col = 1; col < this.strMaze[0].length; col++) {

            this.strMaze[this.strMaze.length - 1][col] = String.format("[%2d]", col - 1);
        }
    }

    private void loadStringMaze() {

        //reset du labyrinthe
        for (Position reset : resetPositions) {

            this.strMaze[reset.getY()][reset.getX()] = "[  ]";
        }

        this.resetPositions.clear();

        //position robot
        int y, x;

        //position probables
        for (PositionProb positionProb : robot.getLastKnowPositions()) {

            y = this.strMaze.length - 2 - positionProb.getPosition().y;

            x = positionProb.getPosition().x + 1;

            this.strMaze[y][x] = "[??]";

            this.resetPositions.add(new Position(y, x));
        }

        y = this.strMaze.length - 2 - this.robotPosition.y;

        x = this.robotPosition.x + 1;

        this.strMaze[y][x] = "[++]";

        this.resetPositions.add(new Position(y, x));
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        loadStringMaze();

        builder.append("CURRENT TIME : "+this.getRobot().getMazeMMC().getTime()+"\n");

        builder.append("CURRENT PERCEPT : "+this.getPercept().toString()+"\n");

        builder.append("ROBOT POSITION : "+this.getRobotPosition()+"\n");

        builder.append("ROBOT POSITIONS KNOWN\n");

        for(PositionProb positionProb : this.robot.getLastKnowPositions()) {

            builder.append(positionProb+"\n");
        }

        for (int row = 0; row < strMaze.length; row++) {

            for (int col = 0; col < strMaze[0].length; col++) {

                builder.append(strMaze[row][col]);
            }

            builder.append('\n');
        }

        return builder.toString();
    }

}
