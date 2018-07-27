package test.dynamic;

import domain.DomainFactory;
import domain.IDomain;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.factory.BatteryDBN;
import network.factory.NetworkFactory;
import org.junit.Test;

import static network.factory.BatteryNetworkFactory.BATTERY_VARS.GAUGE;

public class DBNBatteryTest {

    @Test
    public void testBatteryDBN() {

        NetworkFactory batteryDbn = new BatteryDBN();

        DynamicBayesianNetwork network = batteryDbn.initNetwork();

        int gaugeValues[] = new int[]{5, 5, 5, 5, 5, 4, 4, 4, 0, 4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 0};

        Variable[] gaugeVars = new Variable[gaugeValues.length];

        IDomain batteryLevelDomain = DomainFactory.getBatteryLevelDOmain();

        for (int i = 0; i < gaugeValues.length; i++) {

            Variable obs = new Variable(GAUGE, batteryLevelDomain);

            obs.setValue(gaugeValues[i]);

            gaugeVars[i] = obs;
        }

        network.extend(gaugeVars);

    }

}
