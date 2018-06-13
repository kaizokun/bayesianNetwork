package test.dynamic;

import domain.Domain;
import domain.DomainFactory;
import domain.data.AbstractDouble;
import network.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.RAIN;
import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.UMBRELLA;

public class DynamicBayesianNetworkTest  {

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1Test(){

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1();

        for(int e = 0 ; e < 2 ; e ++) {

            network.extend();
        }

        //initialisation des observations

        Variable umbrella = new Variable(UMBRELLA.toString());

        Variable umbrella1 = network.getVariable(1, umbrella);

        Variable umbrella2 = network.getVariable(2, umbrella);

        umbrella1.setValue(1);

        umbrella2.setValue(1);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        Variable rain2 = network.getVariable(2, rain);

        rain2.setValue(1);

        requests.add(rain2);

        System.out.println(network.toString());

        Map<Domain.DomainValue, AbstractDouble> distribution = network.filter(requests);

        System.out.println();

        System.out.println("request prob : "+distribution);

    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2Test(){

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2();

        for(int e = 0 ; e < 2 ; e ++) {

            network.extend();

            System.out.println(network.toString());

        }

        //initialisation des observations

        Variable umbrella = new Variable(UMBRELLA.toString());

        Variable umbrella1 = network.getVariable(1, umbrella);

        Variable umbrella2 = network.getVariable(2, umbrella);

        umbrella1.setValue(1);

        umbrella2.setValue(1);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        Variable rain2 = network.getVariable(2, rain);

        rain2.setValue(1);

        requests.add(rain2);

        System.out.println(network.toString());

        Map<Domain.DomainValue, AbstractDouble> distribution = network.filter(requests);

        System.out.println();

        System.out.println("request prob : "+distribution);


    }

}
