package network.factory;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.MyDoubleFactory;
import inference.dynamic.Backward;
import inference.dynamic.Forward;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.dynamic.Model;
import java.util.Arrays;

import static network.factory.BatteryNetworkFactory.BATTERY_VARS.*;

public class BatteryDBN extends BatteryNetworkFactory {

    @Override
    public DynamicBayesianNetwork initNetwork() {

        DynamicBayesianNetwork network = new DynamicBayesianNetwork(new MyDoubleFactory());

        network.setForward(new Forward(network));

        network.setBackward(new Backward(network));

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        IDomain batteryLevelDomain = DomainFactory.getBatteryLevelDOmain();

        //TRANSITION
        //Battery root
        ProbabilityCompute tcpBatteryRoot = network.getTCP(
                batteryLevelDomain,
                new Double[][]{{0.0, 0.0, 0.0, 0.0, 0.0, 1.0}});

        Variable batteryRoot = network.addRootVariable(BATTERY.toString(), batteryLevelDomain, tcpBatteryRoot);

        //Gauge Broken root
        ProbabilityCompute tcpGaugeBrokenRoot = network.getTCP(
                batteryLevelDomain,
                new Double[][]{{0.0, 1.0}});//toutes les chances de fonctionner

        Variable gaugeBrokenRoot = network.addRootVariable(BROKEN_GAUGE.toString(), booleanDomain, tcpGaugeBrokenRoot);

        //Battery

        Double[][] batteryTransitionTable = initBatteryTransitionTable(batteryLevelDomain);

        ProbabilityCompute tcpBatteryTransition = network.getTCP(
                batteryLevelDomain, batteryTransitionTable);

        //création du model avec la tcp associé
        Model batteryExtensionModel = new Model(tcpBatteryTransition);
        //ajoute une dependence à la variable avec une profondeur de 1
        batteryExtensionModel.addDependencie(batteryRoot);

        network.addTransitionModel(batteryRoot, batteryExtensionModel);

        //Gauge broken
        //si la gauge est foutu elle le demeure true|true = 100%
        //si elle fonctionne elle a un faible pourcentage de devenir defectueuse true|false = 0.1%

        ProbabilityCompute tcpGaugeBrokenTransition = network.getTCP(
                batteryLevelDomain, new Double[][]{{1.0, 0.0},
                                                   {0.001, 0.999}});

        Model gaugeBrokenExtensionModel = new Model(tcpGaugeBrokenTransition);

        gaugeBrokenExtensionModel.addDependencie(gaugeBrokenRoot);

        network.addTransitionModel(gaugeBrokenRoot, gaugeBrokenExtensionModel);

        //CAPTOR
        //Gauge
        //si la gauge fonctionne elle a 100% de chance d'afficher le bon niveau de batterie
        //0.03 de chance d'afficher 0 si la batterie à un niveau autre et que le cepteur fonctionne : defaillance temporaire
        //si elle est defectueuse elle a 100% de chance d'afficher un niveau 0 : defaillance permanente

        ProbabilityCompute tcpGauge = network.getTCP(
                Arrays.asList(gaugeBrokenRoot, batteryRoot),
                batteryLevelDomain,
                new Double[][]{                         //Battery level, gauge broken state
                        {0.97, 0.0, 0.0, 0.0, 0.0, 0.0}, //0,true
                        {0.03, 0.97, 0.0, 0.0, 0.0, 0.0}, //1,true
                        {0.03, 0.0, 0.97, 0.0, 0.0, 0.0}, //2,true
                        {0.03, 0.0, 0.0, 0.97, 0.0, 0.0}, //3,true
                        {0.03, 0.0, 0.0, 0.0, 0.97, 0.0}, //4,true
                        {0.03, 0.0, 0.0, 0.0, 0.0, 0.97}, //5,true
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //0,false
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //1,false
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //2,false
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //3,false
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //4,false
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}});//5,false

        Variable gauge = new Variable(GAUGE, batteryLevelDomain);

        Model gaugeExtensionModel = new Model(tcpGauge);

        gaugeExtensionModel.addDependencies(gaugeBrokenRoot, batteryRoot);

        network.addCaptorModel(gauge, gaugeExtensionModel);

        return network;
    }

}
