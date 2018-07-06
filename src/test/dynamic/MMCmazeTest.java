package test.dynamic;

import agent.MazeRobot;

import domain.data.MyDoubleFactory;
import environment.Maze;
import environment.Position;
import inference.dynamic.mmc.SmoothingForwardBackwardMMC;

import network.BayesianNetworkFactory;
import network.dynamic.MMC;
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

        MMC mmcMaze = BayesianNetworkFactory.getMazeMMC(maze);

        mmcMaze.setSmoothingMMC(new SmoothingForwardBackwardMMC(mmcMaze, mmcMaze.getForwardMMC(), mmcMaze.getBackwardMMC()));

        //mmcMaze.setSmootStart(5);

        MazeRobot robot = new MazeRobot(mmcMaze, maze);

        boolean lookUp = true;

        do {

            robot.lookUpPosition();

            //System.out.println("BEST POSITIONS : "+robot.getLastKnowPositions());

            // System.out.println("ROBOT POSITIONS DISTRIB: " + robot.getPositionsDistribs().get(mmcMaze.getTime()));

            if (robot.positionKnown()) {

                lookUp = false;

            } else {

                // System.out.println("ROBOT POSITION : " + maze.getRobotPosition());

                robot.move();

                //System.out.println("LAST MOVE "+robot.getMoves().getLast());

                // System.out.println("ROBOT POSITION AFTER MOVE : " + maze.getRobotPosition());

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
