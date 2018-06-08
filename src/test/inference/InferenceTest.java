package test.inference;

import network.BayesianNetwork;
import network.BayesianNetworkFactory;
import network.Variable;

import java.util.ArrayList;
import java.util.List;

import static network.BayesianNetworkFactory.ALARM_NETWORK_VARS.*;

public class InferenceTest {

    public static TestBayesianNetwork alarmTest(){

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


    public static class TestBayesianNetwork {

        public BayesianNetwork bayesianNetwork;

        public List<Variable> request, obs;

        public TestBayesianNetwork(BayesianNetwork bayesianNetwork, List<Variable> request, List<Variable> obs) {
            this.bayesianNetwork = bayesianNetwork;
            this.request = request;
            this.obs = obs;
        }
    }

}
