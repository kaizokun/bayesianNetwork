package test.dynamic;

import environment.Maze;
import environment.Position;
import network.BayesianNetworkFactory;
import org.junit.Test;

public class MMCmazeTest {

    @Test
    public void mazeInitTest() {

        String[] strMaze = new String[]{
                "    #     #   # ",
                "##  # ## # # ###",
                "#   # ##     ## ",
                "  #   #    #    "};

        Maze maze = new Maze(16, 4, strMaze, new Position(3, 1));

        BayesianNetworkFactory.getMazeMMC(maze);


    }

}
