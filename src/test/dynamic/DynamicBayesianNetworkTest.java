package test.dynamic;

import network.BayesianNetworkFactory;
import network.dynamic.DynamicBayesianNetwork;
import org.junit.Test;

public class DynamicBayesianNetworkTest  {

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1Test(){

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1();

        for(int e = 0 ; e < 5 ; e ++) {

            network.extend();

            System.out.println(network.toString());

        }

    }


    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2Test(){

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2();

        for(int e = 0 ; e < 3 ; e ++) {

            network.extend();

            System.out.println(network.toString());

        }

    }

}
