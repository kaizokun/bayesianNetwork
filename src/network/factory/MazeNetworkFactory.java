package network.factory;

import agent.MazeRobot;
import environment.Maze;
import environment.Percept;
import network.dynamic.DynamicBayesianNetwork;

import java.util.LinkedList;
import java.util.List;

public abstract class MazeNetworkFactory {

    public abstract DynamicBayesianNetwork initNewNetwork(Maze maze, MazeRobot robot);

    public abstract DynamicBayesianNetwork initNewNetwork(Maze maze, MazeRobot robot, int time);

    public enum MAZE_NETWORK_VARS {

        POSITION, CAPTOR_POSITION
    }

    protected Double[][] loadTcpMazeRoot(List<MazeRobot.PositionProb> reachablePositions) {

        Double[][] rootTransition = new Double[1][reachablePositions.size()];

        int pos = 0;
        //verifier si l'ordre correspond
        for (MazeRobot.PositionProb positionProb : reachablePositions) {

            rootTransition[0][pos] = positionProb.getProb().getDoubleValue();

            pos++;
        }

        return rootTransition;
    }

    protected Double[][] loadTcpMazeTransition(List<MazeRobot.PositionProb> reachablePositions, Maze maze) {

        Double[][] transition = new Double[reachablePositions.size()][reachablePositions.size()];

        List<MazeRobot.PositionProb> reachablePosChild = new LinkedList<>(reachablePositions);

        int a = 0;

        for (MazeRobot.PositionProb previousPos : reachablePositions) {

            int b = 0;

            for (MazeRobot.PositionProb position : reachablePosChild) {

                if (position.getPosition().adjacent(previousPos.getPosition())) {

                    transition[a][b] = 1.0 / maze.totalAdjacent(previousPos.getPosition());

                } else {

                    transition[a][b] = 0.1;
                }
                b++;
            }
            a++;
        }

        return transition;
    }

    protected Double[][] loadTcpCaptor(Percept[] percepts, List<MazeRobot.PositionProb> reachablePositions, Maze maze) {

        Double[][] captor = new Double[reachablePositions.size()][percepts.length];

        int a = 0;

        for (MazeRobot.PositionProb positionParent : reachablePositions) {

            int b = 0;

            for (Percept percept : percepts) {

                if (maze.getPercept(positionParent.getPosition()).match(percept)) {

                    captor[a][b] = 0.999;

                } else {

                    captor[a][b] = 0.0;
                }

                b++;
            }

            a++;
        }

        return captor;
    }


}
