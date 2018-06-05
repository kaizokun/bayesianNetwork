package network;

import domain.DomainFactory;
import domain.IDomain;
import domain.data.MyDoubleFactory;

import java.util.ArrayList;
import java.util.List;

import static network.BayesianNetworkFactory.ALARM_NETWORK_VARS.*;

public class BayesianNetworkFactory {

    public enum ALARM_NETWORK_VARS{

        CAMBRIOLAGE, TREMBLEMENT_DE_TERRE, ALARM, JEAN_CALL, MARIE_CALL
    }

    public static BayesianNetwork getAlarmNetwork(){

        //reseau

        BayesianNetwork network = new BayesianNetwork(new MyDoubleFactory());

        //domain.Domain booleen commun à toutes les variables

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        //----------------- cambriolage

        ProbabilityCompute tcpCambriolage = network.getTCP(
                booleanDomain,
                new Double[][]{{0.001, 1 - 0.001}});

        Variable cambriolage = network.addRootVariable(CAMBRIOLAGE.toString(), booleanDomain, tcpCambriolage);

        //----------------- tremblement de terre

        ProbabilityCompute tcpTremblementDeTerre = network.getTCP(
                booleanDomain,
                new Double[][]{{0.002, 1 - 0.002}});

        Variable tremblementDeTerre = network.addRootVariable(TREMBLEMENT_DE_TERRE.toString(), booleanDomain, tcpTremblementDeTerre);

        //----------------- alarm

        List<Variable> alarmDependencies = new ArrayList<>();

        alarmDependencies.add(cambriolage);

        alarmDependencies.add(tremblementDeTerre);

        ProbabilityCompute tcpAlarm = network.getTCP(
                alarmDependencies, booleanDomain,
                new Double[][]{
                        {0.95, 1 - 0.95},
                        {0.94, 1 - 0.94},
                        {0.29, 1 - 0.29},
                        {0.001, 1- 0.001}});

        Variable alarm = network.addVariable(ALARM.toString(), booleanDomain, tcpAlarm, alarmDependencies);

        //----------------- jean

        List<Variable> jeanDependencies = new ArrayList<>();

        jeanDependencies.add(alarm);

        ProbabilityCompute tcpJean = network.getTCP(
                jeanDependencies, booleanDomain,
                new Double[][]{
                        {0.90, 1 - 0.90},
                        {0.05, 1 - 0.05}});

        network.addVariable(JEAN_CALL.toString(), booleanDomain, tcpJean, jeanDependencies);

        //----------------- marie

        List<Variable> marieDependencies = new ArrayList<>();

        marieDependencies.add(alarm);

        ProbabilityCompute tcpMarie = network.getTCP(
                marieDependencies, booleanDomain,
                new Double[][]{
                        {0.70, 1 - 0.70},
                        {0.01, 1 - 0.01}});

        network.addVariable(MARIE_CALL.toString(), booleanDomain, tcpMarie, marieDependencies);

        return network;

    }
}
