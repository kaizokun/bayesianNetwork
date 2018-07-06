package test.dynamic;

import agent.MazeRobot;
import agent.MazeRobot.PositionProb;
import domain.data.MyDoubleFactory;
import environment.Maze;
import environment.Position;
import inference.dynamic.mmc.SmoothingForwardBackwardMMC;
import network.BayesianNetworkFactory;
import network.dynamic.MMC;
import org.junit.Test;

import java.util.List;


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

        Maze maze = new Maze( strMaze, new Position(2, 2), new MyDoubleFactory());

        MMC mmcMaze = BayesianNetworkFactory.getMazeMMC(maze);

        mmcMaze.setSmoothingMMC(new SmoothingForwardBackwardMMC(mmcMaze, mmcMaze.getForwardMMC(), mmcMaze.getBackwardMMC()));

       //mmcMaze.setSmootStart(5);

        MazeRobot robot = new MazeRobot(mmcMaze, maze);

        System.out.println("Robot position : "+maze.getRobotPosition());

        boolean lookUp = true;

        do {

            robot.lookUpPosition();

            if(robot.positionKnown()){

                lookUp = false;

                //récupération les dernieres positions possibles du robot
                List<PositionProb> lastKnownPositions = robot.getLastKnowPositions();
                //recuperer toutes les positions alentours
                List<PositionProb> newInitPositions = maze.getNewReachablePosition(lastKnownPositions);
                //enregistre ces positions dans le labyrinthe
                //afin de reinitialiser un mmc avec ces positions
                maze.setReachablePositions(newInitPositions);

            }else{

                robot.move();
            }

        } while (lookUp);

        int time = 1;

        for( ; time < robot.getPositions().size() ; time ++){

            System.out.println("TIME "+time+" : "+robot.getPositions().get(time));
        }

        System.out.println(("TIME "+time+" : "+maze.getRobotPosition()));

        System.out.println(robot.getMoves());

        System.out.println(mmcMaze.getSmoothings());
    }

}
