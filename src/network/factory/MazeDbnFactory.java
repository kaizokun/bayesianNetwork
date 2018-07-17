package network.factory;

import agent.MazeRobot;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.MyDoubleFactory;
import environment.Maze;
import environment.Percept;
import environment.PerceptWall;
import inference.dynamic.Backward;
import inference.dynamic.Forward;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.dynamic.Model;

import java.util.List;

import static java.util.Arrays.asList;
import static network.factory.MazeNetworkFactory.MAZE_NETWORK_VARS.*;

public class MazeDbnFactory extends MazeNetworkFactory {

    @Override
    public DynamicBayesianNetwork initNewNetwork(Maze maze, MazeRobot robot) {

        return initNewNetwork(maze, robot, 0);
    }

    @Override
    public DynamicBayesianNetwork initNewNetwork(Maze maze, MazeRobot robot, int time) {

        DynamicBayesianNetwork network = new DynamicBayesianNetwork(new MyDoubleFactory(), time);

        robot.setDbnMaze(network);

        network.setForward(new Forward(network));

        network.setBackward(new Backward(network));

        //---------------DOMAINES

        //---positions atteignables

        List<MazeRobot.PositionProb> reachablePositions = robot.getReachablePositions();

        IDomain positionDomain = DomainFactory.getMazePositionDomain(robot);

        //---percepts

        Percept[] percepts = PerceptWall.getAllPercepts();

        IDomain captorDomain = DomainFactory.getMazeWallCaptorDomain(percepts);

        //---------------VARIABLES
        //permet d'identifier une variable seul le label est utile pour la methode hashcode et equals
        //ainsi que le domaine pour generer les TCP
        Variable position = new Variable(POSITION.toString(), positionDomain);

        Variable positionCaptor = new Variable(CAPTOR_POSITION.toString(), captorDomain);

        //--------------- ETAT RACINE

        //-------------matrice racine
        Double[][] rootTransition = loadTcpMazeRoot(reachablePositions);

        ProbabilityCompute tcpPositions0 = network.getTCP(
                positionDomain, rootTransition);

        //-------------variable etat racine

        Variable positionRoot = network.addRootVariable(POSITION.toString(), positionDomain, tcpPositions0);

        //--------------- ETAT

        //-------------matrice transition

        Double[][] transition = loadTcpMazeTransition(reachablePositions, maze);

        ProbabilityCompute tcpPositions = network.getTCP(
                asList(position), positionDomain, transition);

        //-------------modele extension etat

        Model positionExtensionModel = new Model(tcpPositions);

        positionExtensionModel.addDependencie(positionRoot);

        network.addTransitionModel(positionRoot, positionExtensionModel);

        //-------------matrice capteur

        Double[][] captor = loadTcpCaptor(percepts, reachablePositions, maze);

        ProbabilityCompute tcpCaptors = network.getTCP(
                asList(position), captorDomain, captor);

        //-------------modele extension capteur

        Model captorExtensionModel = new Model(tcpCaptors);

        captorExtensionModel.addDependencie(position);

        network.addCaptorModel(positionCaptor, captorExtensionModel);

        return network;
    }
}
