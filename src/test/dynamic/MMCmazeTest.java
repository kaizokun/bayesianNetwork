package test.dynamic;

import agent.MazeRobot;

import domain.data.MyDoubleFactory;
import environment.Maze;
import environment.Position;

import network.BayesianNetworkFactory;
import org.junit.Test;

public class MMCmazeTest {

    @Test
    public void mazeInitTest() {

        String[] strMaze = new String[]{
                "    #     #   # ## # ##",
                "##  # ## # # ####   # #",
                "#   # ##     ###  # # #",
                "# #   #  # #      # #  ",
                "    #   ## ## ## ##   #",
                "### # # ## #  ##    #  "};

        Maze maze = new Maze(strMaze, new Position(2, 4), new MyDoubleFactory());

        MazeRobot robot = new MazeRobot(maze);

        BayesianNetworkFactory.initMazeMMC(maze, robot);

        //mmcMaze.setSmootStart(5);

        boolean lookUp = true;

        //System.out.println(maze);

        do {

            robot.lookUpPosition();

            if (robot.positionKnown()) {

                lookUp = false;

            } else {

                robot.move();

                robot.reload();
            }

            System.out.println(maze);

        } while (lookUp);

        int time = 1;

        for (; time < robot.getPositions().size(); time++) {

            System.out.println("TIME " + time + " : " + robot.getPositions().get(time));
        }

        System.out.println(("LAST TIME POSITION " + time + " : " + maze.getRobotPosition()));

        System.out.println(robot.getMoves());

    }

}
