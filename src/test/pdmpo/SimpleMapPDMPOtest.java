package test.pdmpo;

import decision.PDMPO;
import decision.PDMPOSimpleMap;
import decision.PDMPOexploration;
import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import environment.Cardinal;
import environment.SimpleMap;
import math.Distribution;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.factory.MazeRobotRDDFactory;
import org.junit.Test;

import java.util.Random;

import static java.util.Arrays.asList;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.POSITION;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.WALL_PERCEPT;

public class SimpleMapPDMPOtest {

    @Test
    public void initAndExtendNetworkTest() {

        String map[] = new String[]{"   +",
                " # -",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map);

        DynamicBayesianNetwork dynamicBayesianNetwork = new MazeRobotRDDFactory(simpleMap).initNetwork();

        dynamicBayesianNetwork.extend();

        dynamicBayesianNetwork.extend();

        dynamicBayesianNetwork.extend();

        System.out.println(dynamicBayesianNetwork);
    }

    @Test
    public void forwardTest() {

        String map[] = new String[]{"   +",
                " # -",
                "    "};

        Random rdm = new Random();

        SimpleMap simpleMap = new SimpleMap(map);

        IDomain domainPercept = DomainFactory.getMazeWallCaptorDomain(),
                domainMove = DomainFactory.getCardinalDomain();

        DynamicBayesianNetwork dynamicBayesianNetwork = new MazeRobotRDDFactory(simpleMap).initNetwork();

        System.out.println(dynamicBayesianNetwork);

        Distribution distribution = dynamicBayesianNetwork.forward(
                asList(new Variable(POSITION)),
                asList(new Variable(MOVE)), 0, null);

        System.out.println(distribution);

        dynamicBayesianNetwork.extend();

        //initialisation d'une action et d'un percept pour mise à jour de l'état de croyance

        //création de la variable action
        Variable move = new Variable(MOVE);
        //initialisation de la valeur de domaine de la variable action au hazard
        move.setDomainValue(domainMove.getDomainValue(Cardinal.values()[rdm.nextInt(Cardinal.values().length)]));
        //initialisation de la variable move du temps 1
        dynamicBayesianNetwork.initVar(0, move);

        //création de la variable action
        Variable percept = new Variable(WALL_PERCEPT);
        //initialisation de la valeur de domaine de la variable percept au hazard
        Domain.DomainValue perceptValue = domainPercept.getValues().get(rdm.nextInt(domainPercept.getSize()));

        percept.setDomainValue(perceptValue);
        //initialisation de la variable percept au temps 1
        dynamicBayesianNetwork.initVar(1, percept);

        System.out.println(dynamicBayesianNetwork);

        //calcul distribution temps 1
        distribution = dynamicBayesianNetwork.forward(
                asList(new Variable(POSITION)),
                asList(new Variable(MOVE)), 1, distribution);

        System.out.println(distribution);

        //System.out.println(distribution.normalize());
    }

    @Test
    public void PDMPOexplorationPerceptAVGTest() {

        String map[] = new String[]{"   +",
                " # -",
                "    "};

        Random rdm = new Random();

        SimpleMap simpleMap = new SimpleMap(map);

        DynamicBayesianNetwork dynamicBayesianNetwork = new MazeRobotRDDFactory(simpleMap).initNetwork();

        System.out.println(dynamicBayesianNetwork);

        PDMPO pdmpo = new PDMPOSimpleMap(simpleMap, dynamicBayesianNetwork.getDoubleFactory());

        Distribution initForward = dynamicBayesianNetwork.forward(pdmpo.getStates(), pdmpo.getActions(), 0, null);

        System.out.println(initForward);

        PDMPOexploration pdmpoExploration = new PDMPOexploration();

        PDMPOexploration.PDMPOsearchResult result = pdmpoExploration.getBestAction(dynamicBayesianNetwork, pdmpo, initForward, 2);

        System.out.println(result);

        System.out.println("LEAFS "+PDMPOexploration.cptLeaf);
    }

    @Test
    public void PDMPOexplorationPerceptSamplingTest() {

        String map[] = new String[]{"   +",
                " # -",
                "    "};

        Random rdm = new Random();

        SimpleMap simpleMap = new SimpleMap(map);

        DynamicBayesianNetwork dynamicBayesianNetwork = new MazeRobotRDDFactory(simpleMap).initNetwork();

        System.out.println(dynamicBayesianNetwork);

        PDMPO pdmpo = new PDMPOSimpleMap(simpleMap, dynamicBayesianNetwork.getDoubleFactory());

        Distribution initForward = dynamicBayesianNetwork.forward(pdmpo.getStates(), pdmpo.getActions(), 0, null);

        System.out.println(initForward);

        PDMPOexploration pdmpoExploration = new PDMPOexploration();

        PDMPOexploration.PDMPOsearchResult result = pdmpoExploration.getBestActionPerceptSampling(dynamicBayesianNetwork, pdmpo, initForward, 5);

        System.out.println(result);

        System.out.println("LEAFS "+PDMPOexploration.cptLeaf);
    }


}
