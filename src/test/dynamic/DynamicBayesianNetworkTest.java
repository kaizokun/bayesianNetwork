package test.dynamic;

import domain.Domain;
import domain.DomainFactory;
import domain.data.AbstractDouble;
import inference.dynamic.Forward;
import inference.dynamic.Smoothing;
import network.factory.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static network.factory.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.RAIN;
import static network.factory.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.UMBRELLA;

public class DynamicBayesianNetworkTest {

    private void DynamicBayesianNetworkUmbrellaTest(DynamicBayesianNetwork network, int extensions) {

        for (int e = 0; e < extensions; e++) {

            network.extend();
        }

        //initialisation des observations

        Variable umbrella = new Variable(UMBRELLA.toString());

        for (int time = 1; time <= extensions; time++) {

            Variable umbrellaO = network.getVariable(time, umbrella);

            umbrellaO.setValue(1);
        }
    }

    private void DynamicBayesianNetworkUmbrellaTestMax(DynamicBayesianNetwork network) {

        for (int e = 0; e < 5; e++) {

            network.extend();
        }

        Object[] obsValues = new Object[]{1, 0, 1, 0, 1};

        //initialisation des observations

        Variable umbrella = new Variable(UMBRELLA.toString());

        for (int time = 1; time <= 5; time++) {

            Variable umbrellaO = network.getVariable(time, umbrella);

            umbrellaO.setValue(obsValues[time - 1]);
        }
    }

    private void DynamicBayesianNetworkUmbrellaTestSmoothing(DynamicBayesianNetwork network, int extensions,  int markovOrder) {

        this.DynamicBayesianNetworkUmbrellaTest(network, extensions);

        //initialisation de la requete

        Variable rain = new Variable(RAIN.toString());

        Variable rainReq = network.getVariable(1, rain);

        rainReq.setValue(1);

        System.out.println(network.toString());

        Smoothing smoothing = new Smoothing(network);

        AbstractDouble rs = smoothing.smoothing(rainReq, markovOrder);

        System.out.println();

        System.out.println("request prob : " + rs);

        System.out.println();
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1SmoothingTest() {

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1();

        this.DynamicBayesianNetworkUmbrellaTestSmoothing(network, 3, 1);

        //network.showForwardDistributions();

        //network.showBackwardDistributions();
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2SmoothingTest() {

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2();

        this.DynamicBayesianNetworkUmbrellaTestSmoothing(network, 4, 2);

       // network.showForwardDistributions();

       // network.showBackwardDistributions();

       // network.showFullBackwardDistributions();
    }

    private void DynamicBayesianNetworkUmbrellaTestFilter(DynamicBayesianNetwork network, int extensions) {

        this.DynamicBayesianNetworkUmbrellaTest(network, extensions);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        Variable rainReq = network.getVariable(extensions, rain);

        rainReq.setValue(1);

        requests.add(rainReq);

        Forward forward = new Forward(network);

        AbstractDouble rs = forward.forwardAsk(requests);

        System.out.println();

        System.out.println("request prob : " + rs);

        System.out.println();

        forward.showForward();
    }

    private void DynamicBayesianNetworkUmbrellaTestFilterAndMax(DynamicBayesianNetwork network) {

        this.DynamicBayesianNetworkUmbrellaTestMax(network);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        Variable rainReq = network.getVariable(5, rain);

        rainReq.setValue(1);

        requests.add(rainReq);

        Forward forward = new Forward(network);

        forward.forward(requests, true);

        System.out.println();
        System.out.println("MAX");
        System.out.println();

        forward.showMax();

        System.out.println();
        System.out.println("ALL PATH");
        System.out.println();

        Map<String, Map<Domain.DomainValue, List<Variable>>> mostLikelyPath = forward.getMostLikelyPath();

        for (String key : mostLikelyPath.keySet()) {

            System.out.println("STATE " + key);

            for (Map.Entry<Domain.DomainValue, List<Variable>> entry : mostLikelyPath.get(key).entrySet()) {

                System.out.println(entry.getKey() + " " + entry.getValue());
            }
        }

        System.out.println();
        System.out.println("MOST LIKELY PATH");
        System.out.println();

        List<Variable> mostLikelySequence = forward.computeMostLikelyPath(requests);

        for(Variable states : mostLikelySequence){

            System.out.println(states);
        }

    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1FilterMax() {

        this.DynamicBayesianNetworkUmbrellaTestFilterAndMax(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1());
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1FilterTest() {

        this.DynamicBayesianNetworkUmbrellaTestFilter(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1(), 2);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2FilterTest() {

        this.DynamicBayesianNetworkUmbrellaTestFilter(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2(), 2);

    }

    private void DynamicBayesianNetworkUmbrellaTestPredict(DynamicBayesianNetwork network, int extensions, int predictTime) {

        this.DynamicBayesianNetworkUmbrellaTest(network, extensions);

        //initialisation de la requete

        List<Variable> requests = new LinkedList<>();

        Variable rainReq = new Variable(RAIN.toString(), DomainFactory.getBooleanDomain());

        rainReq.setValue(0);

        requests.add(rainReq);

        Forward forward = new Forward(network);

        AbstractDouble rs = forward.prediction(requests, predictTime);

        System.out.println();

        System.out.println("request prob : " + rs);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder1PredictTest() {

        this.DynamicBayesianNetworkUmbrellaTestPredict(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1(), 10, 15);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrder2PredictTest() {

        this.DynamicBayesianNetworkUmbrellaTestPredict(BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder2(), 10, 15);
    }

    @Test
    public void DynamicBayesianNetworkUmbrellaOrderForwardBackwardTest() {

        DynamicBayesianNetwork network = BayesianNetworkFactory.getUmbrellaDynamicNetworkOrder1();

        this.DynamicBayesianNetworkUmbrellaTest(network, 3);

        Smoothing smoothing = new Smoothing(network);

        List<Variable> req = new LinkedList<>();

        Variable rain = new Variable(RAIN.toString());

        req.add(rain);

        smoothing.forwardBackward(rain, 0, 3);
    }

}
