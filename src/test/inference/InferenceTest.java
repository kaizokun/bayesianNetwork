package test.inference;

import domain.data.AbstractDouble;
import inference.approximation.SamplingAsk;
import inference.approximation.SimilaritySamplingAsk;
import inference.exact.EliminationAsk;
import inference.exact.EnumerationAsk;
import network.BayesianNetwork;
import network.BayesianNetworkFactory;
import network.Variable;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

import static network.BayesianNetworkFactory.ALARM_NETWORK_VARS.*;

public class InferenceTest {


    //@Test
    public void rdmNumbers(){

        for(int i = 0 ; i < 10000 ; i ++){

            double rdm = new Random().nextDouble();

            if(rdm < 0.001) {

                System.out.println(rdm);
            }
        }
    }

    @Test
    @Ignore
    public void alarmTestEliminationAsk() {

        System.out.println();
        System.out.println("====================");
        System.out.println("ELIMINATION ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        long t1 = System.currentTimeMillis();

        AbstractDouble rs = EliminationAsk.ask(test.request,test.obs,test.bayesianNetwork);

        System.out.println("TEMPS "+(System.currentTimeMillis() - t1));

        System.out.println("RESULTAT : " + rs);
    }

    @Test
    public void alarmTestEnumerationAsk() {

        System.out.println();
        System.out.println("====================");
        System.out.println("ENUMERATION ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        long t1 = System.currentTimeMillis();

        AbstractDouble rs = EnumerationAsk.ask(test.request, test.obs, test.bayesianNetwork);

        System.out.println("TEMPS "+(System.currentTimeMillis() - t1));

        System.out.println("RESULTAT : " + rs);
    }

    @Test
    public void alarmTestSampleAsk(){

        System.out.println();
        System.out.println("====================");
        System.out.println("SAMPLE ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        long t1 = System.currentTimeMillis();

        AbstractDouble rs = SamplingAsk.ask(test.request, test.obs, test.bayesianNetwork, 500000);

        System.out.println("TEMPS "+(System.currentTimeMillis() - t1));

        System.out.println("RESULTAT : " + rs);
    }

    @Test
    public void alarmTestSimilarSampleAsk(){

        System.out.println();
        System.out.println("====================");
        System.out.println("SIMILAR ASK");
        System.out.println("====================");

        TestBayesianNetwork test = alarmTest();

        long t1 = System.currentTimeMillis();

        AbstractDouble rs = SimilaritySamplingAsk.ask(test.request, test.obs, test.bayesianNetwork, 500000);

        System.out.println("TEMPS "+(System.currentTimeMillis() - t1));

        System.out.println("RESULTAT : " + rs);
    }


    private TestBayesianNetwork alarmTest(){

        BayesianNetwork alarmNetwork = BayesianNetworkFactory.getAlarmNetwork();

        Variable marieCall = alarmNetwork.getVariable(MARIE_CALL.toString());

        Variable jeanCall = alarmNetwork.getVariable(JEAN_CALL.toString());

        Variable cambriolage = alarmNetwork.getVariable(CAMBRIOLAGE.toString());

        //Variable tremblementDeTerre = alarmNetwork.getVariable(TREMBLEMENT_DE_TERRE.toString());

        //observations

        List<Variable> obs = new ArrayList<>();

        marieCall.setValue(1);

        jeanCall.setValue(1);

        obs.add(marieCall);

        obs.add(jeanCall);

        //requete

        List<Variable> request = new ArrayList<>();

        cambriolage.setValue(1);

        //tremblementDeTerre.setValue(0);

        request.add(cambriolage);

        //request.add(tremblementDeTerre);

        return new TestBayesianNetwork(alarmNetwork, request, obs);

    }


    private class TestBayesianNetwork {

        public BayesianNetwork bayesianNetwork;

        public List<Variable> request, obs;

        public TestBayesianNetwork(BayesianNetwork bayesianNetwork, List<Variable> request, List<Variable> obs) {
            this.bayesianNetwork = bayesianNetwork;
            this.request = request;
            this.obs = obs;
        }
    }

}
