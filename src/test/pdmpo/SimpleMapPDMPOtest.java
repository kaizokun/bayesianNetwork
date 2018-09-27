package test.pdmpo;

import environment.SimpleMap;
import network.dynamic.DynamicBayesianNetwork;
import network.factory.MazeRobotRDDFactory;
import org.junit.Test;
import test.decision.NotDeterministic.SimpleMapTest;

public class SimpleMapPDMPOtest {


    @Test
    public void initNetworkTest(){

        double discount = 0.999;

        String map[] = new String[]{"   +",
                " # -",
                "    "};

        SimpleMap simpleMap = new SimpleMap(map);

        DynamicBayesianNetwork dynamicBayesianNetwork = new MazeRobotRDDFactory(simpleMap).initNetwork();



    }

}
