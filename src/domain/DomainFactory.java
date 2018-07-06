package domain;

import agent.MazeRobot;
import environment.Cardinal;
import environment.Maze;
import environment.Position;
import math.Combination;

import java.util.List;

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

        for (MazeRobot.PositionProb position : maze.getReachablePositions()) {

            positions[xy] = new Position(position.getPosition().getY(), position.getPosition().getX());

            xy++;
        }

        return new Domain(positions);
    }


    public static IDomain getMazeWallCaptorDomain() {

        return getMazeWallCaptorDomain(Combination.getSubsets(Cardinal.values()));
    }

    public static IDomain getMazeWallCaptorDomain(List<Cardinal>[] percepts) {

        return new Domain(percepts);
    }
}
