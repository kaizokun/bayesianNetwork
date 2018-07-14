package test.dynamic;

import agent.MazeRobot;
import domain.data.MyDoubleFactory;
import environment.Cardinal;
import environment.Maze;
import environment.Position;
import network.factory.MazeDbnFactory;
import network.factory.MazeMMCFactory;
import network.factory.MazeNetworkFactory;
import org.junit.Test;

import java.util.LinkedList;

public class MMCmazeTest {

    @Test
    public void mazeTest(){

        LinkedList<Cardinal> moves =  mazeTest(new MazeMMCFactory(), null);

        mazeTest(new MazeDbnFactory(), moves);
    }

    public  LinkedList<Cardinal> mazeTest(MazeNetworkFactory networkFactory, LinkedList<Cardinal> moves) {

        System.out.println();
        System.out.println("====================================================");
        System.out.println("=======================MAZE TEST====================");
        System.out.println("====================================================");
        System.out.println();

        String[] strMaze = new String[]{
                "    #     #   # ## # ##",
                "##  # ## # # ####   # #",
                "#   # ##     ###  # # #",
                "# #   #  # #      # #  ",
                "    #   ## ## ## ##   #",
                "### # # ## #  ##    #  "};

        Maze maze = new Maze(strMaze, new Position(2, 4), new MyDoubleFactory());

        MazeRobot robot = new MazeRobot(maze, networkFactory);

        robot.setMovesTest(moves);

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

        return robot.getMoves();
    }





}
