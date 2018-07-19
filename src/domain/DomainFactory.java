package domain;

import agent.MazeRobot;
import environment.*;
import math.Combination;

import java.util.List;

public class DomainFactory {

    public static IDomain getBooleanDomain() {

        return new Domain(1, 0);
    }

    public static IDomain getBatteryLevelDOmain() {

        return new Domain(0, 1, 2, 3, 4, 5);
    }

    public static IDomain getABCDDomain() {

        return new Domain('a', 'b', 'c', 'd');
    }

    public static IDomain getMazePositionDomain(MazeRobot robot) {

        Position[] positions = new Position[robot.getReachablePositions().size()];

        int xy = 0;

        for (MazeRobot.PositionProb position : robot.getReachablePositions()) {

            positions[xy] = new Position(position.getPosition().getY(), position.getPosition().getX());

            xy++;
        }

        return new Domain(positions);
    }


    public static IDomain getMazeWallCaptorDomain() {

        return getMazeWallCaptorDomain(PerceptWall.getAllPercepts());
    }

    public static IDomain getMazeWallCaptorDomain(Percept[] percepts) {

        return new Domain(percepts);
    }

}
