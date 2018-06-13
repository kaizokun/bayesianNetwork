package test.dynamic;

import domain.DomainFactory;
import domain.data.AbstractDouble;
import network.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.RAIN;
import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.UMBRELLA;

public class DynamicBayesianNetworkTest  {

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1FilterTest(){

        this.DynamicBayesianNetworkUmbrellaTestFilter(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1(),10);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2FilterTest(){

        this.DynamicBayesianNetworkUmbrellaTestFilter(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2(),10);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1PredictTest(){

        this.DynamicBayesianNetworkUmbrellaTestPredict(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1(),10, 15);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2PredictTest(){

        this.DynamicBayesianNetworkUmbrellaTestPredict(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2(),10,15);
    }

    private void DynamicBayesianNetworkUmbrellaTest( DynamicBayesianNetwork network, int extensions){

        for(int e = 0 ; e < extensions ; e ++) {

            network.extend();
        }

        //initialisation des observations

        Object[] umbrellaValues = new Object[]{1,1,1,1,1,1,1,1,1,1};

        Variable umbrella = new Variable(UMBRELLA.toString());

        for( int time = 1 ; time <= extensions; time ++){

            Variable umbrellaO = network.getVariable(time, umbrella);

            umbrellaO.setValue(umbrellaValues[time - 1]);
        }
    }

    private void DynamicBayesianNetworkUmbrellaTestFilter( DynamicBayesianNetwork network, int extensions){

        this.DynamicBayesianNetworkUmbrellaTest(network, extensions);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        Variable rainReq = network.getVariable(extensions, rain);

        rainReq.setValue(1);

        requests.add(rainReq);

        AbstractDouble rs = network.filter(requests);

        System.out.println();

        System.out.println("request prob : "+rs);
    }

    private void DynamicBayesianNetworkUmbrellaTestPredict( DynamicBayesianNetwork network, int extensions, int predictTime){

        this.DynamicBayesianNetworkUmbrellaTest(network, extensions);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rainReq = new Variable(RAIN.toString(), DomainFactory.getBooleanDomain());

        rainReq.setValue(1);

        requests.add(rainReq);

        AbstractDouble rs = network.predict(requests, predictTime);

        System.out.println();

        System.out.println("request prob : "+rs);
    }




}
