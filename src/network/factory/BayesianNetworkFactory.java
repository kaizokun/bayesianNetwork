package network.factory;

import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDoubleFactory;
import domain.data.MyDoubleFactory;
import inference.dynamic.mmc.*;
import network.BayesianNetwork;
import network.ProbabilityCompute;
import network.ProbabilityComputeFromTCP;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.dynamic.MMC;
import network.dynamic.Model;

import java.util.*;

import static network.factory.BayesianNetworkFactory.ABCD_NETWORK_VARS.*;
import static network.factory.BayesianNetworkFactory.ALARM_NETWORK_VARS.*;
import static network.factory.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.*;

public class BayesianNetworkFactory {

    public enum ABCD_NETWORK_VARS {

        VAR_A
    }

    public static BayesianNetwork getABCDNetwork() {

        //reseau

        BayesianNetwork network = new BayesianNetwork(new MyDoubleFactory());

        //domain.Domain abcd commun à toutes les variables

        IDomain abcdDomain = DomainFactory.getABCDDomain();

        //----------------- var_a

        ProbabilityCompute tcpVarA = network.getTCP(
                abcdDomain,
                new Double[][]{{0.1, 0.4, 0.3, 0.2}});

        network.addRootVariable(VAR_A.toString(), abcdDomain, tcpVarA);

        return network;
    }

    public enum ALARM_NETWORK_VARS {

        CAMBRIOLAGE, TREMBLEMENT_DE_TERRE, ALARM, JEAN_CALL, MARIE_CALL
    }

    public static BayesianNetwork getAlarmNetwork() {

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
                        {0.001, 1 - 0.001}});

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

    public enum UMBRELLA_NETWORK_VARS {

        UMBRELLA, COAT, RAIN, CLOUD,
    }

    public static DynamicBayesianNetwork getUmbrellaDynamicNetworkOrder1() {

        DynamicBayesianNetwork network = new DynamicBayesianNetwork(new MyDoubleFactory());

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        //Rain time 0
        ProbabilityCompute tcpRain0 = network.getTCP(
                booleanDomain,
                new Double[][]{{0.5, 1 - 0.5}});

        Variable rain = network.addRootVariable(RAIN.toString(), booleanDomain, tcpRain0);

        //Models
        //Rain time 1

        //Rain 1 rowVars

        List<Variable> dependencies = new LinkedList<>();

        dependencies.add(rain);

        ProbabilityCompute tcpRain = network.getTCP(
                dependencies,
                booleanDomain,
                new Double[][]{{0.7, 1 - 0.7},
                        {0.3, 1 - 0.3}});
        //création du model avec la tcp associé
        Model rainExtensionModel = new Model(tcpRain);
        //ajoute une dependence à la variable avec une profondeur de 1
        rainExtensionModel.addDependencie(rain);

        network.addTransitionModel(rain, rainExtensionModel);

        //Umbrella time 1

        Variable umbrella = new Variable(UMBRELLA.toString(), booleanDomain);

        ProbabilityCompute tcpUmbrella = network.getTCP(
                dependencies,
                booleanDomain,
                new Double[][]{{0.9, 1 - 0.9},
                        {0.2, 1 - 0.2}});

        Model umbrellaExtensionModel = new Model(tcpUmbrella);

        umbrellaExtensionModel.addDependencie(rain);

        network.addCaptorModel(umbrella, umbrellaExtensionModel);

        return network;
    }

    public static DynamicBayesianNetwork getUmbrellaDynamicNetworkOrder1TwoStates() {

        DynamicBayesianNetwork network = getUmbrellaDynamicNetworkOrder1();

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        //Rain time 0
        ProbabilityCompute tcpCloud0 = network.getTCP(
                booleanDomain,
                new Double[][]{{0.8, 1 - 0.8}});

        Variable cloud = network.addRootVariable(CLOUD.toString(), booleanDomain, tcpCloud0);

        //Cloud 1 rowVars

        List<Variable> dependencies = new LinkedList<>();

        dependencies.add(cloud);

        ProbabilityCompute tcpCloud = network.getTCP(
                dependencies,
                booleanDomain,
                new Double[][]{{0.8, 1 - 0.8},
                        {0.2, 1 - 0.2}});
        //création du model avec la tcp associé
        Model cloudExtensionModel = new Model(tcpCloud);
        //ajoute une dependence à la variable avec une profondeur de 1
        cloudExtensionModel.addDependencie(cloud, 1);

        network.addTransitionModel(cloud, cloudExtensionModel);

        //coat time 1

        Variable coat = new Variable(COAT.toString(), booleanDomain);

        ProbabilityCompute tcpCoat = network.getTCP(
                dependencies,
                booleanDomain,
                new Double[][]{{0.9, 1 - 0.9},
                        {0.4, 1 - 0.4}});

        Model coatExtensionModel = new Model(tcpCoat);

        coatExtensionModel.addDependencie(cloud);

        network.addCaptorModel(coat, coatExtensionModel);

        return network;
    }

    public static MMC getUmbrellaMMCDynamicNetworkTwoVars() {

        AbstractDoubleFactory doubleFactory = new MyDoubleFactory();

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        //Rain time 0
        ProbabilityCompute tcpRain0 = new ProbabilityComputeFromTCP(
                booleanDomain, new Double[][]{{0.5, 1 - 0.5}},
                doubleFactory);

        Variable rain0 = new Variable(RAIN.toString(), booleanDomain, tcpRain0);

        //Cloud time 0
        ProbabilityCompute tcpCloud0 = new ProbabilityComputeFromTCP(
                booleanDomain, new Double[][]{{0.6, 1 - 0.6}},
                doubleFactory);

        Variable cloud0 = new Variable(CLOUD.toString(), booleanDomain, tcpCloud0);

        //Rain time 1

        ProbabilityCompute tcpRain1 = new ProbabilityComputeFromTCP(
                new Variable[]{rain0},
                booleanDomain,
                new Double[][]{{0.7, 1 - 0.7},
                        {0.3, 1 - 0.3}},
                doubleFactory);

        Variable rain1 = new Variable(RAIN.toString(), booleanDomain, tcpRain1, new Variable[]{rain0});

        //Cloud time 1

        ProbabilityCompute tcpCloud1 = new ProbabilityComputeFromTCP(
                new Variable[]{cloud0},
                booleanDomain,
                new Double[][]{{0.8, 1 - 0.8},
                        {0.2, 1 - 0.2}},
                doubleFactory);

        Variable cloud1 = new Variable(CLOUD.toString(), booleanDomain, tcpCloud1, new Variable[]{cloud0});

        //Umbrella time 1

        ProbabilityCompute tcpUmbrella1 = new ProbabilityComputeFromTCP(
                new Variable[]{rain1},
                booleanDomain,
                new Double[][]{{0.9, 1 - 0.9},
                        {0.2, 1 - 0.2}},
                doubleFactory);

        Variable umbrella1 = new Variable(UMBRELLA.toString(), booleanDomain, tcpUmbrella1, new Variable[]{rain1});

        //Coat time 1

        ProbabilityCompute tcpCoat1 = new ProbabilityComputeFromTCP(
                new Variable[]{cloud1},
                booleanDomain,
                new Double[][]{{0.6, 1 - 0.6},
                        {0.3, 1 - 0.3}},
                doubleFactory);

        Variable coat1 = new Variable(COAT.toString(), booleanDomain, tcpCoat1, new Variable[]{cloud1});

        MMC mmc = new MMC(new Variable[]{rain0, cloud0}, new Variable[]{rain1, cloud1}, new Variable[]{umbrella1, coat1}, doubleFactory);

        mmc.setForwardMMC(new ForwardMMC(mmc));

        mmc.setBackwardMMC(new BackwardMMC(mmc));

        mmc.setSmoothingMMC(new SmoothingForwardBackwardMMC(mmc, mmc.getForwardMMC(), mmc.getBackwardMMC()));

        return mmc;
    }

    public static MMC getUmbrellaMMCDynamicNetworkOneVars() {

        AbstractDoubleFactory doubleFactory = new MyDoubleFactory();

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        //Rain time 0
        ProbabilityCompute tcpRain0 = new ProbabilityComputeFromTCP(
                booleanDomain, new Double[][]{{0.5, 1 - 0.5}},
                doubleFactory);

        Variable rain0 = new Variable(RAIN.toString(), booleanDomain, tcpRain0);

        //Rain time 1

        ProbabilityCompute tcpRain1 = new ProbabilityComputeFromTCP(
                new Variable[]{rain0},
                booleanDomain,
                new Double[][]{{0.7, 1 - 0.7},
                        {0.3, 1 - 0.3}},
                doubleFactory);

        Variable rain1 = new Variable(RAIN.toString(), booleanDomain, tcpRain1, new Variable[]{rain0});

        //Umbrella time 1

        ProbabilityCompute tcpUmbrella1 = new ProbabilityComputeFromTCP(
                new Variable[]{rain1},
                booleanDomain,
                new Double[][]{{0.9, 1 - 0.9},
                        {0.2, 1 - 0.2}},
                doubleFactory);

        Variable umbrella1 = new Variable(UMBRELLA.toString(), booleanDomain, tcpUmbrella1, new Variable[]{rain1});

        MMC mmc = new MMC(new Variable[]{rain0}, new Variable[]{rain1}, new Variable[]{umbrella1}, doubleFactory);

        mmc.setForwardMMC(new ForwardMMC(mmc));

        mmc.setBackwardMMC(new BackwardMMC(mmc));

        mmc.setSmoothingMMC(new SmoothingForwardBackwardMMC(mmc, mmc.getForwardMMC(), mmc.getBackwardMMC()));

        return mmc;
    }

    public static DynamicBayesianNetwork getUmbrellaDynamicNetworkOrder2() {

        DynamicBayesianNetwork network = new DynamicBayesianNetwork(new MyDoubleFactory());

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        //Rain time 0
        ProbabilityCompute tcpRain0 = network.getTCP(
                booleanDomain,
                new Double[][]{{0.5, 1 - 0.5}});

        Variable rain = network.addRootVariable(RAIN.toString(), booleanDomain, tcpRain0);

        //Models
        //Rain time 1
        //Rain 1 rain1Dep

        List<Variable> oneRainDep = new LinkedList<>();

        oneRainDep.add(rain);

        ProbabilityCompute tcpRain1 = network.getTCP(
                oneRainDep,
                booleanDomain,
                new Double[][]{{0.7, 1 - 0.7},
                        {0.3, 1 - 0.3}});
        //création du model avec la tcp associé
        Model rainExtensionModel1 = new Model(tcpRain1);
        //ajoute une dependence à la variable avec une profondeur de 1
        rainExtensionModel1.addDependencie(rain, 1);

        //Rain time 2

        //Rain 1 rain1Dep

        List<Variable> twoRainDep = new LinkedList<>();

        twoRainDep.add(rain);

        twoRainDep.add(rain);
        /*
         * VV : pluie les deux jours precedents
         * VF : pluie il y a deux jour mais pas la veille
         * FV : pas de pluie il y a deux jour mais la veille
         * FF : pas de pluie les deux derniers jours
         * */
        ProbabilityCompute tcpRain2 = network.getTCP(
                twoRainDep,
                booleanDomain,
                new Double[][]{{0.9, 1 - 0.9},
                        {0.3, 1 - 0.3},
                        {0.7, 1 - 0.7},
                        {0.1, 1 - 0.9}});
        //création du model avec la tcp associé
        Model rainExtensionModel2 = new Model(tcpRain2);
        //ajoute une dependence à la variable avec une profondeur de 1
        rainExtensionModel2.addDependencie(rain, 2);

        //modeles de transition associés à la variable rain (sont label : equal, hashcode)
        network.addTransitionModel(rain, rainExtensionModel1);

        network.addTransitionModel(rain, rainExtensionModel2);

        //Umbrella time 1

        Variable umbrella = new Variable(UMBRELLA.toString(), booleanDomain);

        ProbabilityCompute tcpUmbrella = network.getTCP(
                oneRainDep,
                booleanDomain,
                new Double[][]{{0.9, 1 - 0.9},
                        {0.2, 1 - 0.2}});

        Model umbrellaExtensionModel = new Model(tcpUmbrella);

        umbrellaExtensionModel.addDependencie(rain);

        network.addCaptorModel(umbrella, umbrellaExtensionModel);

        return network;
    }


}
