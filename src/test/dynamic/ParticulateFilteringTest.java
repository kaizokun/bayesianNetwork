package test.dynamic;

import domain.DomainFactory;
import inference.ParticulateFiltering;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.factory.BayesianNetworkFactory;
import org.junit.Test;

import java.util.Arrays;

import static network.factory.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.*;

public class ParticulateFilteringTest {

    @Test
    public void umbrellaParticulateFilteringTest() {

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1();

        Variable state = new Variable(RAIN.toString());

        Variable obs = new Variable(UMBRELLA.toString(), DomainFactory.getBooleanDomain());

        obs.setValue(1);

        ParticulateFiltering.ask(network, Arrays.asList(state), Arrays.asList(obs), 10);
    }

}
