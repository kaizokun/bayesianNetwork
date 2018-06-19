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

    private void DynamicBayesianNetworkUmbrellaTest( DynamicBayesianNetwork network, int extensions){

        for(int e = 0 ; e < extensions ; e ++) {

            network.extend();
        }

        //initialisation des observations

        Variable umbrella = new Variable(UMBRELLA.toString());

        for( int time = 1 ; time <= extensions; time ++){

            Variable umbrellaO = network.getVariable(time, umbrella);

            umbrellaO.setValue(1);
        }
    }

    private void DynamicBayesianNetworkUmbrellaTestMax( DynamicBayesianNetwork network){

        for(int e = 0 ; e < 5 ; e ++) {

            network.extend();
        }

        Object[] obsValues = new Object[]{1, 1, 0, 1, 1};

        //initialisation des observations

        Variable umbrella = new Variable(UMBRELLA.toString());

        for( int time = 1 ; time <= 5; time ++){

            Variable umbrellaO = network.getVariable(time, umbrella);

            umbrellaO.setValue(obsValues[time - 1]);
        }
    }

    private void DynamicBayesianNetworkUmbrellaTestSmoothing(DynamicBayesianNetwork network, int extensions) {

        this.DynamicBayesianNetworkUmbrellaTest(network, extensions);

        //initialisation de la requete

        Variable rain = new Variable(RAIN.toString());

        Variable rainReq = network.getVariable(1, rain);

        rainReq.setValue(1);

        AbstractDouble rs = network.smoothing(rainReq);

        System.out.println();

        System.out.println("request prob : "+rs);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1SmoothingTest(){

        this.DynamicBayesianNetworkUmbrellaTestSmoothing(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1(),2);
    }

    private void DynamicBayesianNetworkUmbrellaTestFilter( DynamicBayesianNetwork network, int extensions){

        this.DynamicBayesianNetworkUmbrellaTest(network, extensions);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        Variable rainReq = network.getVariable(extensions, rain);

        rainReq.setValue(1);

        requests.add(rainReq);

        AbstractDouble rs = network.filtering(requests);

        System.out.println();

        System.out.println("request prob : "+rs);
    }

    private void DynamicBayesianNetworkUmbrellaTestFilterAndMax( DynamicBayesianNetwork network){

        this.DynamicBayesianNetworkUmbrellaTestMax(network);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        Variable rainReq = network.getVariable(5, rain);

        rainReq.setValue(1);

        requests.add(rainReq);

        network.filtering(requests);

        System.out.println();
        System.out.println("MAX");
        System.out.println();

        Map<String, Map<Domain.DomainValue, AbstractDouble>> max = network.getMaxDistribSaved();

        for(String key :  max.keySet()){

            System.out.println("STATE "+key);

            for(Map.Entry<Domain.DomainValue,AbstractDouble> entry : max.get(key).entrySet()){

                System.out.println(entry.getKey()+" "+entry.getValue());
            }
        }

        System.out.println();
        System.out.println("MOST LIKELY PATH");
        System.out.println();

        Map<String, Map<Domain.DomainValue, List<Variable>>> mostLikelyPath = network.getMostLikelyPath();

        for(String key :  mostLikelyPath.keySet()){

            System.out.println("STATE "+key);

            for(Map.Entry<Domain.DomainValue,List<Variable>> entry : mostLikelyPath.get(key).entrySet()){

                System.out.println(entry.getKey()+" "+entry.getValue());
            }
        }
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1FilterMax(){

        this.DynamicBayesianNetworkUmbrellaTestFilterAndMax(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1());
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1FilterTest(){

        this.DynamicBayesianNetworkUmbrellaTestFilter(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1(),2);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2FilterTest(){

        this.DynamicBayesianNetworkUmbrellaTestFilter(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2(),3);
    }

    private void DynamicBayesianNetworkUmbrellaTestPredict( DynamicBayesianNetwork network, int extensions, int predictTime){


        this.DynamicBayesianNetworkUmbrellaTest(network, extensions);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rainReq = new Variable(RAIN.toString(), DomainFactory.getBooleanDomain());

        rainReq.setValue(1);

        requests.add(rainReq);

        AbstractDouble rs = network.prediction(requests, predictTime);

        System.out.println();

        System.out.println("request prob : "+rs);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1PredictTest(){

        this.DynamicBayesianNetworkUmbrellaTestPredict(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1(),10, 15);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2PredictTest(){

        this.DynamicBayesianNetworkUmbrellaTestPredict(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2(),10,15);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrderForwardBackwardTest(){

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1();

        this.DynamicBayesianNetworkUmbrellaTest(network, 3);

        List<Variable> req = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        req.add(rain);

        network.forwardBackward(rain, 0,3);
    }

}
