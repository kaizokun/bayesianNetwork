package test.dynamic;

import agent.MazeRobot;
import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import environment.Cardinal;
import environment.Maze;
import environment.Position;
import math.Matrix;
import network.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.MMC;
import org.junit.Test;

import java.util.Arrays;

import static environment.Cardinal.NORTH;
import static environment.Cardinal.SOUTH;
import static environment.Cardinal.WEST;
import static network.BayesianNetworkFactory.MAZE_NETWORK_VARS.CAPTOR_POSITION;

public class MMCmazeTest {

    @Test
    public void mazeInitTest() {

        String[] strMaze = new String[]{
                "    #     #   # ",
                "##  # ## # # ###",
                "#   # ##     ## ",
                "  #   #    #    "};

        Maze maze = new Maze(16, 4, strMaze, new Position(3, 0));

        MMC mmcMaze = BayesianNetworkFactory.getMazeMMC(maze);

        MazeRobot robot = new MazeRobot(mmcMaze, maze);

        robot.nextStep();

    }

}
