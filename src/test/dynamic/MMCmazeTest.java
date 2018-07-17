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
import test.TestUtil;

import java.util.LinkedList;

public class MMCmazeTest {

    public LinkedList<Cardinal> moves;

    @Test
    public void mazeTest() {

        TestUtil.initTime();

        mazeTest(new MazeMMCFactory());

        TestUtil.printTimeDelta();

        TestUtil.initTime();

        mazeTest(new MazeDbnFactory());

        TestUtil.printTimeDelta();
    }

    //@Test
    public void mazeMMCTest() {

        mazeTest(new MazeMMCFactory());
    }

   // @Test
    public void mazeDBNTest() {

        mazeTest(new MazeDbnFactory());
    }

    public void mazeTest(MazeNetworkFactory networkFactory) {

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

                //robot.reload();
            }

            System.out.println(maze);

        } while (lookUp);

        int time = 1;

        for (; time < robot.getPositions().size(); time++) {

            System.out.println("TIME " + time + " : " + robot.getPositions().get(time));
        }

        System.out.println(("LAST TIME POSITION " + time + " : " + maze.getRobotPosition()));

        System.out.println("DEPLACEMENTS : "+robot.getMoves());

        System.out.println("POSITIONS REELES : "+maze.getRobotPositions());

        System.out.println("MOST LIKELY PATH "+robot.getDbnMaze().getMostLikelyPath());

        this.moves = robot.getMoves();
    }


}
