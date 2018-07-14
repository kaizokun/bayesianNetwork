package test.dynamic;

import agent.MazeRobot;
import domain.data.MyDoubleFactory;
import environment.Maze;
import environment.Position;
import network.factory.MazeDbnFactory;
import network.factory.MazeMMCFactory;
import network.factory.MazeNetworkFactory;
import org.junit.Test;

public class MMCmazeTest {

    @Test
    public void mazeTestMMC(){

        mazeTest(new MazeMMCFactory());
    }

    @Test
    public void mazeTestDBN(){

        mazeTest(new MazeDbnFactory());
    }

    public void mazeTest(MazeNetworkFactory networkFactory) {

        String[] strMaze = new String[]{
                "    #     #   # ## # ##",
                "##  # ## # # ####   # #",
                "#   # ##     ###  # # #",
                "# #   #  # #      # #  ",
                "    #   ## ## ## ##   #",
                "### # # ## #  ##    #  "};

        Maze maze = new Maze(strMaze, new Position(2, 4), new MyDoubleFactory());

        MazeRobot robot = new MazeRobot(maze, networkFactory);

        //mmcMaze.setSmootStart(5);

        boolean lookUp = true;

        System.out.println(maze);

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
