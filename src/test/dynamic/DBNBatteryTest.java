package dynamic;

import network.dynamic.DynamicBayesianNetwork;
import network.factory.BatteryDBN;
import network.factory.BatteryNetworkFactory;
import network.factory.NetworkFactory;
import org.junit.Test;

public class DBNBatteryTest {

    @Test
    public void testBatteryDBN(){

        NetworkFactory batteryDbn = new BatteryDBN();

        DynamicBayesianNetwork network = batteryDbn.initNetwork();

    }

}
