package agent;

import environment.Maze;
import network.dynamic.MMC;

public class MazeRobot {

    protected MMC mazeMMC;

    protected Maze maze;

    public MazeRobot(MMC mazeMMC, Maze maze) {

        this.mazeMMC = mazeMMC;

        this.maze = maze;
    }


}
