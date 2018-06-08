package test.sampling;

import network.BayesianNetwork;
import network.BayesianNetworkFactory;
import network.Variable;
import org.junit.Test;

import java.util.LinkedList;

import static network.BayesianNetworkFactory.ABCD_NETWORK_VARS.VAR_A;
import static network.BayesianNetworkFactory.ALARM_NETWORK_VARS.*;

public class SamplingTest {

    @Test
    public void markovCumulativeFrequenciesTest(){

        BayesianNetwork alarmNetwork = BayesianNetworkFactory.getAlarmNetwork();

        Variable alarm = alarmNetwork.getVariable(ALARM.toString());

        alarm.initCumulativeMarkovFrequencies(new LinkedList<>());

        //alarm.showCumulativeMarkovFrequencies();
    }


    @Test
    public void markovLoadTest(){

        BayesianNetwork alarmNetwork = BayesianNetworkFactory.getAlarmNetwork();

        Variable alarm = alarmNetwork.getVariable(ALARM.toString());

        alarm.loadMarkovCover();
    }

    @Test
    public void abcdSampleTest(){

        BayesianNetwork abcdNetwork = BayesianNetworkFactory.getABCDNetwork();

        Variable varA = abcdNetwork.getVariable(VAR_A.toString());

        varA.setValue('d');

        varSample(100000, varA);
    }

    @Test
    public void alarmSampleTest() {

        BayesianNetwork alarmNetwork = BayesianNetworkFactory.getAlarmNetwork();

        Variable alarm = alarmNetwork.getVariable(ALARM.toString());

        Variable cambriolage = alarmNetwork.getVariable(CAMBRIOLAGE.toString());

        Variable tremblementDeTerre = alarmNetwork.getVariable(TREMBLEMENT_DE_TERRE.toString());

        //dependencies

        cambriolage.setValue(0);

        tremblementDeTerre.setValue(1);

        //var

        alarm.setValue(1);

        varSample(5000, alarm);
    }

    private void varSample(int samples, Variable var){

        Object value = var.getValue();

        int totatValue = 0;

        for( int s = 0 ; s < samples ; s ++){

            var.initRdmValue();

            if(var.getValue().equals(value)){

                totatValue ++;
            }
        }

        System.out.println("Frequence : "+((double)totatValue / samples));
    }


}
