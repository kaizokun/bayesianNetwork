import domain.data.AbstractDouble;
import inference.exact.EliminationAsk;
import inference.exact.EnumerationAsk;
import network.BayesianNetwork;
import network.BayesianNetworkFactory;
import network.Variable;

import java.util.*;

import static network.BayesianNetworkFactory.ALARM_NETWORK_VARS.*;

public class Main {

    public static void main(String[] args) {

        enumElimComp();
        //varImportantForRequestTest1();
        //varImportantForRequestTest2();
/*
        alarmTestEliminationAsk();

        alarmTestEnumerationAsk();
        */
    }

    public static void varImportantForRequestTest1() {

        BayesianNetwork alarmNetwork = BayesianNetworkFactory.getAlarmNetwork();

        Variable marieCall = alarmNetwork.getVariable(MARIE_CALL.toString());

        Variable jeanCall = alarmNetwork.getVariable(JEAN_CALL.toString());

        Variable cambriolage = alarmNetwork.getVariable(CAMBRIOLAGE.toString());

        marieCall.setValue(1);

        jeanCall.setValue(1);

        cambriolage.setValue(1);

        List<Variable> obs = new LinkedList<>();

        List<Variable> req = new LinkedList<>();

        req.add(cambriolage);

        obs.add(marieCall);
        obs.add(jeanCall);

        BayesianNetwork.markImportantVars(req, obs);

        List<Variable> topo = alarmNetwork.getTopologicalOrder();

        System.out.println(topo);

    }

    public static void varImportantForRequestTest2() {

        BayesianNetwork alarmNetwork = BayesianNetworkFactory.getAlarmNetwork();

        Variable alarm = alarmNetwork.getVariable(ALARM.toString());

        Variable cambriolage = alarmNetwork.getVariable(CAMBRIOLAGE.toString());

        alarm.setValue(1);

        cambriolage.setValue(1);

        List<Variable> obs = new LinkedList<>();

        List<Variable> req = new LinkedList<>();

        obs.add(alarm);

        req.add(cambriolage);

        BayesianNetwork.markImportantVars(req, obs);

        List<Variable> topo = alarmNetwork.getTopologicalOrder();

        System.out.println(topo);

    }

    public static void enumElimComp() {

        long t1 = System.currentTimeMillis();

        alarmTestEnumerationAsk();

        long t2 = System.currentTimeMillis();

        System.out.println(" Temps " + (t2 - t1));

        t1 = System.currentTimeMillis();

        alarmTestEliminationAsk();

        t2 = System.currentTimeMillis();

        System.out.println(" Temps " + (t2 - t1));

    }
/*
    public static void factorTest() {

        BayesianNetwork alarmNetwork = BayesianNetworkFactory.getAlarmNetwork();

        Variable marieCall = alarmNetwork.getVariable(MARIE_CALL.toString());

        Variable jeanCall = alarmNetwork.getVariable(JEAN_CALL.toString());

        Variable alarm = alarmNetwork.getVariable(ALARM.toString());

        marieCall.setValue(1);

        jeanCall.setValue(1);

        //pour eliminationAsk sauvegarder la valeur de la requete et clear la variable
        //pour generer les facteurs
        //cambriolage.setValue(1);

        try {

            Factor factorMarie = new Factor(marieCall);

            Factor factorJean = new Factor(jeanCall);

            Factor factorAlarm = new Factor(alarm);

            System.out.println(factorJean);

            System.out.println(factorMarie);

            System.out.println(factorAlarm);

            Set<Factor> factorSet = new LinkedHashSet<>();

            factorSet.add(factorMarie);

            factorSet.add(factorJean);

            factorSet.add(factorAlarm);

            System.out.println("-------------------------------------");

            Factor.variableElimination(factorSet, alarm, false);

            for (Factor factor : factorSet) {

                System.out.println(factor);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }


    }
*/


    private static TestBayesianNetwork alarmTest(){

        BayesianNetwork alarmNetwork = BayesianNetworkFactory.getAlarmNetwork();

        Variable marieCall = alarmNetwork.getVariable(MARIE_CALL.toString());

        Variable jeanCall = alarmNetwork.getVariable(JEAN_CALL.toString());

        Variable cambriolage = alarmNetwork.getVariable(CAMBRIOLAGE.toString());

        Variable tremblementDeTerre = alarmNetwork.getVariable(TREMBLEMENT_DE_TERRE.toString());

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

    public static void alarmTestEliminationAsk() {

        TestBayesianNetwork test = alarmTest();

        AbstractDouble rs = EliminationAsk.ask(test.request,test.obs,test.bayesianNetwork);

        System.out.println("RESULTAT : " + rs);

    }

    public static void alarmTestEnumerationAsk() {

        TestBayesianNetwork test = alarmTest();

        AbstractDouble rs = EnumerationAsk.ask(test.request, test.obs, test.bayesianNetwork);

        System.out.println("RESULTAT : " + rs);

    }

    private static class TestBayesianNetwork {

        public BayesianNetwork bayesianNetwork;

        public List<Variable> request, obs;

        public TestBayesianNetwork(BayesianNetwork bayesianNetwork, List<Variable> request, List<Variable> obs) {
            this.bayesianNetwork = bayesianNetwork;
            this.request = request;
            this.obs = obs;
        }
    }

}
