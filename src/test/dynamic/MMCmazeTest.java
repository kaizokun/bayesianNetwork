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
                "    #     #   # ## # ##",
                "##  # ## # # ####   # #",
                "#   # ##     ###  # # #",
                "# #   #  # #      # #  ",
                "    #   ## ## ## ##   #",
                "### # # ## #  ##    #  "};

        Maze maze = new Maze( strMaze, new Position(2, 2));

        MMC mmcMaze = BayesianNetworkFactory.getMazeMMC(maze);

        MazeRobot robot = new MazeRobot(mmcMaze, maze);

        System.out.println("Robot position : "+maze.getRobotPosition());

        boolean lookUp = true;

        do {

            robot.lookUpPosition();

            if(robot.positionKnown()){

                lookUp = false;

            }else{

                robot.move();
            }

        } while (lookUp);

        System.out.println(robot.getMoves());
    }

}
