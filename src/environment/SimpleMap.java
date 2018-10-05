package environment;

import decision.Politic;

import java.util.*;

public class SimpleMap implements Environment<Position> {

    private static final char WALL = '#', GOAL = '+', HOLE = '-';

    private String[] map;

    private Position goodExit, badExit;

    private Set<Position> notFinalStates = new HashSet<>(),
            finalStates = new HashSet<>(), allStates = new HashSet<>();

    private Set<Position> walls = new HashSet<>();

    private int xLimit, yLimit;

    private Position agentPosition;

    private Set<Position> riskyPositions = new HashSet<>();

    protected Map<Position, PerceptWall> positionPercept = new Hashtable<>();

    public SimpleMap(String[] map) {

        this.map = map;

        this.xLimit = map[0].length();

        this.yLimit = map.length;

        for (int iRow = 0; iRow < map.length; iRow++) {

            String row = map[iRow];

            for (int iCol = 0; iCol < row.length(); iCol++) {

                char symbol = row.charAt(iCol);

                int y = this.yLimit - iRow, x = iCol + 1;

                if (symbol == WALL) {

                    this.walls.add(new Position(y, x));

                } else if (symbol == GOAL) {

                    this.goodExit = new Position(y, x);

                } else if (symbol == HOLE) {

                    this.badExit = new Position(y, x);

                } else {

                    this.notFinalStates.add(new Position(this.yLimit - iRow, iCol + 1));
                }
            }
        }

        this.finalStates.add(this.goodExit);

        this.finalStates.add(this.badExit);

        this.notFinalStates.removeAll(this.finalStates);

        this.allStates.addAll(this.notFinalStates);

        this.allStates.addAll(this.finalStates);

        this.initRiskyPositions();
    }

    private void initRiskyPositions() {

        for (Position nearPosition : badExit.getNearbyPositions()) {

            if (isPositionReachable(nearPosition)) {

                riskyPositions.add(nearPosition);
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

    public PerceptWall getPercept(Position position) {

        //creer un set de direction correspondant aux obstacles de la position

        if (positionPercept.containsKey(position)) {

            return positionPercept.get(position);
        }

        PerceptWall perceptWall = new PerceptWall();

        for (Cardinal cardinal : Cardinal.values()) {

            if (!isPositionReachable(position.move(cardinal))) {

                perceptWall.addWallDirection(cardinal);
            }
        }

        positionPercept.put(position, perceptWall);

        return perceptWall;
    }

    public PerceptWall getAgentPercept() {

        return this.getPercept(this.agentPosition);
    }

    public void moveAgent(DirectionMove move) {

        Position newPosition = this.agentPosition.move(move);

        if (isPositionReachable(newPosition)) {

            this.agentPosition = newPosition;
        }
    }

    @Override
    public List<DirectionMove> getActions(Position state) {

        List<DirectionMove> actions = new LinkedList<>();

        for (DirectionMove direction : DirectionMove.values()) {

            Position adjPos = state.move(direction);

            if (isPositionReachable(adjPos)) {

                actions.add(direction);
            }
        }

        return actions;
    }

    @Override
    public Set<Position> getNotFinalStates() {

        return notFinalStates;
    }

    public Set<? extends State> getFinalStates() {

        return finalStates;
    }

    public boolean isFinalState(Position position) {

        return this.finalStates.contains(position);
    }


    public Set<Position> getAllStates() {
        return allStates;
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

    public Position getAgentPosition() {
        return agentPosition;
    }

    public String getPoliticMap(Politic politic) {

        StringBuilder builder = new StringBuilder();

        char[] movSymbols = new char[]{'^', '>', 'v', '<'};

        Map<State, String> statesSymbols = new Hashtable<>();

        for (State state : notFinalStates) {

            Action action = politic.getAction(state);

            DirectionMove directionMove = (DirectionMove) action;

            statesSymbols.put(state, "[" + movSymbols[directionMove.ordinal()] + "]");
        }

        statesSymbols.put(badExit, "[-]");

        statesSymbols.put(goodExit, "[+]");

        for (State wall : walls) {

            statesSymbols.put(wall, "[#]");
        }

        for (int y = yLimit; y > 0; y--) {

            for (int x = 1; x <= xLimit; x++) {

                builder.append(statesSymbols.get(new Position(y, x)));
            }

            builder.append('\n');
        }

        return builder.toString();
    }

    public Set<Position> getRiskyPositions() {

        return riskyPositions;
    }

    public void setAgentPosition(Position agentPosition) {
        this.agentPosition = agentPosition;
    }

    public void initRdmAgentPosition() {

        this.setAgentPosition(this.agentPosition = new ArrayList<>(this.notFinalStates).get(new Random().nextInt(this.notFinalStates.size())));
    }
}
