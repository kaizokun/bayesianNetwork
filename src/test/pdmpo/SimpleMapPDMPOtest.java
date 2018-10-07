package test.pdmpo;

import decision.*;
import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.MyDoubleFactory;
import environment.*;
import math.Distribution;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.factory.SimpleMapRDDFactory;
import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;
import static network.factory.SimpleMapRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.SimpleMapRDDFactory.SIMPLE_MAP_VARS.POSITION;
import static network.factory.SimpleMapRDDFactory.SIMPLE_MAP_VARS.WALL_PERCEPT;

public class SimpleMapPDMPOtest {

    @Test
    public void initAndExtendNetworkTest() {

        String map[] = new String[]{"   +",
                " # -",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map);

        DynamicBayesianNetwork dynamicBayesianNetwork = new SimpleMapRDDFactory(simpleMap).initNetwork();

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

        DynamicBayesianNetwork dynamicBayesianNetwork = new SimpleMapRDDFactory(simpleMap).initNetwork();

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

    public Politic PDMPOexplorationPerceptTest(DynamicBayesianNetwork explorationNetwork,
                                               DynamicBayesianNetwork agentNetwork,
                                               PDMPOexploration pdmpoSearch,
                                               SimpleMap simpleMap,
                                               PDMPO pdmpo,
                                               int firstDepth,
                                               double limitSec,
                                               boolean showLog) {


        //fourni le reseau à l'algo d'exploration
        pdmpoSearch.setNetwork(explorationNetwork);
        //fourni le PDMPO à l'algo d'exploration
        pdmpoSearch.setPdmpo(pdmpo);
        //premiere extension du reseau de l'agent
        agentNetwork.extend();
        //initialisation du premier percepts et d'une action sans deplacement pour effectuer un forward
        Variable perceptVar = pdmpo.getPerceptVar();
        //l'environement fourni le percept
        perceptVar.setValue(simpleMap.getAgentPercept());

        Variable actionVar = pdmpo.getActionVar();

        actionVar.setValue(DirectionMove.ON_THE_SPOT);

        agentNetwork.initVar(0, actionVar);

        agentNetwork.initVar(1, perceptVar);

        Distribution stateOfBelieve = agentNetwork.forward(pdmpo.getStates(), pdmpo.getActions(), 1, null);

        //LinkedList<DirectionMove> actions = new LinkedList<>();

        // LinkedList<Position> positions = new LinkedList<>();

        if (showLog) {

            System.out.println("----------- DEPART ---------");

            System.out.println(stateOfBelieve);
        }

        Map<Position, DirectionMove> politic = new Hashtable<>();

        int moves = 0;

        do {

            if (showLog) {

                System.out.println("AGENT REAL POSITION " + simpleMap.getAgentPosition());

                System.out.println("AGENT REAL PERCEPT " + simpleMap.getAgentPercept());
            }
            //exploration à partir de l'état de croyance courant retourne une action ayant la plus haute utilité
            PDMPOexploration.PDMPOsearchResult result = pdmpoSearch.getBestAction(stateOfBelieve, firstDepth, limitSec);

            politic.put(simpleMap.getAgentPosition(), (DirectionMove) result.getAction().getValue());

            //positions.add(simpleMap.getAgentPosition());

            //actions.add((DirectionMove) result.getAction().getValue());

            if (showLog)
                System.out.println("BEST ACTION " + result);

            //deplace l'agent dans l'environnement
            simpleMap.moveAgent((DirectionMove) result.getAction().getValue());

            if (showLog)
                System.out.println("AGENT NEW REAL POSITION " + simpleMap.getAgentPosition());

            //recupère le percept courant de l'agant
            PerceptWall perceptWall = simpleMap.getAgentPercept();
            //met à jour l'état de croyance de l'agent
            //1.etend le reseau
            agentNetwork.extend();
            //2.recupere la variable action et initialise sa valeur
            actionVar.setDomainValue(result.getAction());
            //3.initialise la variable action au temps avant extension dans le reseau
            agentNetwork.initVar(agentNetwork.getTime() - 1, actionVar);
            //4.recupere la variable percept et initialise sa valeur
            perceptVar.setValue(perceptWall);
            //5.initialise la variable percept au temps courant dans le reseau
            agentNetwork.initVar(agentNetwork.getTime(), perceptVar);

            stateOfBelieve = agentNetwork.forward(pdmpo.getStates(), pdmpo.getActions(), agentNetwork.getTime(), stateOfBelieve);

            if (showLog) {

                System.out.println("AGENT STATE OF BELIEVE ");

                System.out.println(stateOfBelieve);
            }

            moves++;

        } while (!simpleMap.getAgentPosition().equals(simpleMap.getGoodExit()) && moves < 20);

        return new Politic() {
            @Override
            public Action getAction(State state) {
                return politic.get(state);
            }

            @Override
            public void setAction(Action action, State state) {
            }
        };
    }

    private String map1[] = new String[]{
            "   +",
            " # -",
            "    "
    };

    private String map2[] = new String[]{
            "   # +",//3
            " #   -",//2
            "   #  " //1
    };     //123456

    private String map3[] = new String[]{
            "#   ##  + ",//5
            "# # #   - ",//4
            "# ##### # ",//3
            "   #    # ",//2
            " #   ##   " //1
          // 1234567891
            //         0
    };

    @Test
    public void PDMPOexplorationPerceptAVGAllPositionsTest() {

        //réduire les percepts fonctionne bien pour ce cas ci

        //environnement
        SimpleMap simpleMap = new SimpleMap(map3);
        //reseau baysien utilié pour l'exploration
        DynamicBayesianNetwork explorationNetwork = new SimpleMapRDDFactory(simpleMap).initNetwork();
        //reseau bayesien utilisé par l'agent
        DynamicBayesianNetwork agentNetwork = new SimpleMapRDDFactory(simpleMap).initNetwork();

        //estimation : ne fonctionne pas avec des murs qui rendent l'estimation à vol d'oiseau
        //erronée (une meilleur estimation pourrait faire mieux) par exemple avec la map3 et depart position(4,4)
        //sans estimation ca passe.
        PDMPOexploration search = new PDMPOexplorationFullPercept(
                new MyDoubleFactory(), 0.05, 0.75, 0.3,
                0.3, false);

        //PDMPO simplemap
        PDMPO pdmpo = new PDMPOSimpleMap(simpleMap, new MyDoubleFactory(), 1, 1);

        for (Position position : simpleMap.getNotFinalStates()) {

            System.out.println("-------TEST " + position + "------");

            simpleMap.setAgentPosition(position);

            Politic politic = PDMPOexplorationPerceptTest(explorationNetwork,
                    agentNetwork, search, simpleMap, pdmpo,
                    0, 10, false);

            System.out.println("POLITIC : \n" + simpleMap.getPoliticMap(politic));

            search.resetSharedResults();
        }
    }

    @Test
    public void PDMPOexplorationPerceptAVGOnePositionsTest() {

        //environnement
        SimpleMap simpleMap = new SimpleMap(map3);
        //reseau baysien utilié pour l'exploration
        DynamicBayesianNetwork explorationNetwork = new SimpleMapRDDFactory(simpleMap).initNetwork();
        //reseau bayesien utilisé par l'agent
        DynamicBayesianNetwork agentNetwork = new SimpleMapRDDFactory(simpleMap).initNetwork();

        PDMPOexploration search = new PDMPOexplorationFullPercept(
                new MyDoubleFactory(), 0.05, 0.75, 0.2, 0.2, false);

        //PDMPO simplemap
        PDMPO pdmpo = new PDMPOSimpleMap(simpleMap, new MyDoubleFactory(), 0.8, 1);

        simpleMap.setAgentPosition(new Position(1, 1));

        System.out.println("START FROM : " + simpleMap.getAgentPosition());

        PDMPOexplorationPerceptTest(explorationNetwork, agentNetwork, search, simpleMap, pdmpo, 0, 1, false);
    }

    @Test
    public void PDMPOexplorationPerceptSamplingTest() {

        //algorithme qui ne fait des prevision de percepts que sur un seul echantilloné
        //à partir de la distribution sur les percepts, maleuresement parfois il echantillone des percepts trop improbable
        //au detriment des plus probables

        //environnement
        SimpleMap simpleMap = new SimpleMap(map1);
        //reseau baysien utilié pour l'exploration
        DynamicBayesianNetwork explorationNetwork = new SimpleMapRDDFactory(simpleMap).initNetwork();
        //reseau bayesien utilisé par l'agent
        DynamicBayesianNetwork agentNetwork = new SimpleMapRDDFactory(simpleMap).initNetwork();

        PDMPOexploration search = new PDMPOexplorationSamplingPercept(
                new MyDoubleFactory(), 0.0, 0.1, 0.2, false);
        //PDMPO simplemap
        PDMPO pdmpo = new PDMPOSimpleMap(simpleMap, new MyDoubleFactory(), 0.6, 1);

        PDMPOexplorationPerceptTest(explorationNetwork, agentNetwork, search, simpleMap, pdmpo, 6, 2, true);
    }

}
