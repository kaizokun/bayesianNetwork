package test.dynamic;

import agent.MazeRobot;
import environment.Maze;
import environment.Position;
import network.BayesianNetworkFactory;
import network.dynamic.MMC;
import org.junit.Test;

public class MMCmazeTest {

    @Test
    public void mazeInitTest() {

        String[] strMaze = new String[]{
                "    #     #   # ",
                "##  # ## # # ###",
                "#   # ##     ## ",
                "  #   #    #    "};

        Maze maze = new Maze(16, 4, strMaze, new Position(2, 2));

        MMC mmcMaze = BayesianNetworkFactory.getMazeMMC(maze);

        MazeRobot robot = new MazeRobot(mmcMaze, maze);

        while(!robot.positionKnown()) {

            robot.nextStep();
        }

        System.out.println(robot.getPositions());

        System.out.println(robot.getMoves());
    }

}
