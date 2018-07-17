package network.factory;

import agent.MazeRobot;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDoubleFactory;
import domain.data.MyDoubleFactory;
import environment.Maze;
import environment.Percept;
import environment.PerceptWall;
import inference.dynamic.mmc.BackwardMMC;
import inference.dynamic.mmc.ForwardMMC;
import inference.dynamic.mmc.MostLikelySequencyMMC;
import inference.dynamic.mmc.SmoothingForwardBackwardMMC;
import network.ProbabilityCompute;
import network.ProbabilityComputeFromTCP;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.dynamic.MMC;

import java.util.List;

import static network.factory.MazeNetworkFactory.MAZE_NETWORK_VARS.*;


public class MazeMMCFactory extends MazeNetworkFactory {

    @Override
    public DynamicBayesianNetwork initNewNetwork(Maze maze, MazeRobot robot) {

        return initNewNetwork(maze, robot, 0);
    }

    @Override
    public DynamicBayesianNetwork initNewNetwork(Maze maze, MazeRobot robot, int time) {

        AbstractDoubleFactory doubleFactory = new MyDoubleFactory();

        List<MazeRobot.PositionProb> reachablePositions = robot.getReachablePositions();

        IDomain positionDomain = DomainFactory.getMazePositionDomain(robot);

        //-------------matrice racine
        Double[][] rootTransition = loadTcpMazeRoot(reachablePositions);

        ProbabilityCompute tcpPositions0 = new ProbabilityComputeFromTCP(
                positionDomain, rootTransition, doubleFactory);

        //-------------variable etat racine

        Variable position0 = new Variable(POSITION, positionDomain, tcpPositions0);

        //-------------matrice transition

        Double[][] transition = loadTcpMazeTransition(reachablePositions, maze);

        ProbabilityCompute tcpPositions = new ProbabilityComputeFromTCP(
                new Variable[]{position0}, positionDomain, transition, doubleFactory);

       // System.out.println("TCP "+time);

        //System.out.println(tcpPositions);

        //-------------variable etat

        Variable position = new Variable(POSITION, positionDomain, tcpPositions, new Variable[]{position0});

        //------------- capteur

        /*
         * les lignes sont les positionsDistrib atteignables
         * les colones les sous ensembles de positionsDistrib N S E W soit 2^4
         * la probabilité d'un percept est total si cela correspond aux contours d'une position
         * on ajoute un peu de bruit pour ne pas avoir de valer 0 par exemple 0.01
         * */

        //List<Cardinal>[] percepts = Combination.getSubsets(Cardinal.values());

        Percept[] percepts = PerceptWall.getAllPercepts();

        Double[][] captor = loadTcpCaptor(percepts, reachablePositions, maze);

        IDomain captorDomain = DomainFactory.getMazeWallCaptorDomain(percepts);

        ProbabilityCompute tcpCaptors = new ProbabilityComputeFromTCP(
                new Variable[]{position}, captorDomain, captor, doubleFactory);

        //-------------variable capteur position

        Variable positionCaptor = new Variable(CAPTOR_POSITION, captorDomain, tcpCaptors, new Variable[]{position});

        //-----------------------------------------

        MMC mmc = new MMC(new Variable[]{position0}, new Variable[]{position}, new Variable[]{positionCaptor}, doubleFactory, time);

        mmc.setForwardMMC(new ForwardMMC(mmc));

        mmc.setMostLikelySequence(new MostLikelySequencyMMC(mmc));

        mmc.setBackwardMMC(new BackwardMMC(mmc));

        mmc.setSmoothingMMC(new SmoothingForwardBackwardMMC(mmc, mmc.getForwardMMC(), mmc.getBackwardMMC()));

        robot.setDbnMaze(mmc);

        return mmc;
    }

}
