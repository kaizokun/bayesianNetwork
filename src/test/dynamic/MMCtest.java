package test.dynamic;

import network.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.CLOUD;
import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.RAIN;
import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.UMBRELLA;

public class MMCtest {

    @Test
    public void megaVariableTestTwoVars(){

    }

    @Test
    public void megaVariableTestOneVar(){

       BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

    }

}
