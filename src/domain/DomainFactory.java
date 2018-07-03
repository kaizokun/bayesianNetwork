package domain;

import environment.Cardinal;
import environment.Maze;
import environment.Position;

public class DomainFactory {

    public static IDomain getBooleanDomain() {

        return new Domain(1, 0);
    }

    public static IDomain getABCDDomain() {

        return new Domain('a', 'b', 'c', 'd');
    }

    public static IDomain getMazePositionDomain(Maze maze) {

        Position[] positions = new Position[maze.countReachablePositions()];

        int xy = 0;

        for (Position position : maze.getReachablePositions()) {

            positions[xy] = new Position(position.getY(), position.getX());

            xy++;
        }

        return new Domain(positions);
    }

    public static IDomain getCardinalsDomain() {

        return new Domain(Cardinal.NORTH, Cardinal.SOUTH, Cardinal.EAST, Cardinal.WEST);
    }

}
