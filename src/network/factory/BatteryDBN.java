package network.factory;


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

        //TRANSITION INITIAL TIME 0
        //Battery root
        //niveau de  0 à 5, au départ le niveau 5 est à 100% : batterie pleine
        ProbabilityCompute tcpBatteryRoot = network.getTCP(
                batteryLevelDomain,
                new Double[][]{{0.0, 0.0, 0.0, 0.0, 0.0, 1.0}});

        // System.out.println("TCP BATTERY ROOT "+tcpBatteryRoot);

        Variable batteryRoot = network.addRootVariable(BATTERY.toString(), batteryLevelDomain, tcpBatteryRoot);

        //Gauge Broken root
        //fonctionne ou pas, domaine bolleen
        //ici on demarre avec une gauge qui fonctionne donc 100% de chance qu'elle ne soit pas brisé
        ProbabilityCompute tcpGaugeBrokenRoot = network.getTCP(
                booleanDomain,
                new Double[][]{{0.0, 1.0}});

        //  System.out.println("TCP GAUGE BROKEN ROOT "+tcpGaugeBrokenRoot);

        Variable gaugeBrokenRoot = network.addRootVariable(BROKEN_GAUGE.toString(), booleanDomain, tcpGaugeBrokenRoot);

        //TRANSITION TIME > 0
        //Battery

        Double[][] batteryTransitionTable = initBatteryTransitionTable(batteryLevelDomain);

        ProbabilityCompute tcpBatteryTransition = network.getTCP(
                Arrays.asList(batteryRoot),
                batteryLevelDomain, batteryTransitionTable);

        // System.out.println("TCP BATTERY "+tcpBatteryTransition);

        //création du model avec la tcp associé
        Model batteryExtensionModel = new Model(tcpBatteryTransition);
        //ajoute une dependence à la variable avec une profondeur de 1
        batteryExtensionModel.addDependencie(batteryRoot);

        network.addTransitionModel(batteryRoot, batteryExtensionModel);

        //Gauge broken
        //si la gauge est foutu elle le demeure true|true = 100%
        //si elle fonctionne elle a un faible pourcentage de devenir defectueuse true|false = 0.1%

        ProbabilityCompute tcpGaugeBrokenTransition = network.getTCP(
                Arrays.asList(gaugeBrokenRoot),
                booleanDomain, new Double[][]{{1.0, 0.0},
                        {0.001, 0.999}});

        //  System.out.println("TCP GAUGE BROKEN "+tcpGaugeBrokenTransition);

        Model gaugeBrokenExtensionModel = new Model(tcpGaugeBrokenTransition);

        gaugeBrokenExtensionModel.addDependencie(gaugeBrokenRoot);

        network.addTransitionModel(gaugeBrokenRoot, gaugeBrokenExtensionModel);

        //CAPTOR
        //Gauge
        //si la gauge fonctionne elle a 100% de chance d'afficher le bon niveau de batterie
        //ou plutot 97% pour syntétiser une erreur temporaire soit
        //0.03 de chance d'afficher 0 si la batterie à un niveau autre et que le capteur fonctionne : defaillance temporaire
        //qui doit être supérieur à la probabilité de transition d'un état élévée à 0 pour être utile.
        //si elle est defectueuse elle a 100% de chance d'afficher un niveau 0 : defaillance permanente

        ProbabilityCompute tcpGauge = network.getTCP(
                Arrays.asList(gaugeBrokenRoot, batteryRoot),
                batteryLevelDomain,
                new Double[][]{                         //Battery level, gauge broken state
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //true,0
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //true,1
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //true,2
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //true,3
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //true,4
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //true,5
                        {1.0, 0.0, 0.0, 0.0, 0.0, 0.0}, //false,0
                        {0.03, 0.97, 0.0, 0.0, 0.0, 0.0}, //false,1
                        {0.03, 0.0, 0.97, 0.0, 0.0, 0.0}, //false,2
                        {0.03, 0.0, 0.0, 0.97, 0.0, 0.0}, //false,3
                        {0.03, 0.0, 0.0, 0.0, 0.97, 0.0}, //false,4
                        {0.03, 0.0, 0.0, 0.0, 0.0, 0.97}});//false,5

        // System.out.println("TCP GAUGE "+tcpGauge);

        Variable gauge = new Variable(GAUGE, batteryLevelDomain);

        Model gaugeExtensionModel = new Model(tcpGauge);

        gaugeExtensionModel.addDependencies(gaugeBrokenRoot, batteryRoot);

        network.addCaptorModel(gauge, gaugeExtensionModel);

        return network;
    }

}
