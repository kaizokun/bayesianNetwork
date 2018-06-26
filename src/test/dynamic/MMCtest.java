package test.dynamic;

import network.BayesianNetwork;
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

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkTwoVars();

        //l'étendre une fois
        network.extend();

        List<Variable> varsToMerge = new LinkedList<>();

        varsToMerge.add(network.getVariable(RAIN.toString()));

        varsToMerge.add(network.getVariable(CLOUD.toString()));

        Variable megaRootVar = network.mergeStateVariables(0, varsToMerge);

        Variable megaVar = network.mergeStateVariables(1, varsToMerge);

        System.out.println(megaRootVar.getMatrixView());

        System.out.println(megaVar.getMatrixView());
    }

    @Test
    public void megaVariableTestOneVar(){

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkTwoVars();

        //l'étendre une fois
        network.extend();

        //etats

        List<Variable> varsState = new LinkedList<>();

        varsState.add(network.getVariable(0, new Variable(RAIN.toString())));

        Variable megaRootStateVar = network.mergeStateVariables(0, varsState);

        Variable megaStateVar = network.mergeStateVariables(1, varsState);

        System.out.println(megaRootStateVar.getMatrixView());

        System.out.println(megaStateVar.getMatrixView());

        //observations

        List<Variable> varsObs = new LinkedList<>();

        varsObs.add(network.getVariable(1, new Variable(UMBRELLA.toString())));

        Variable megaObsVar = network.mergeObservationVariable(1, varsObs, varsState);

        System.out.println(megaObsVar.getMatrixView());

    }

}
