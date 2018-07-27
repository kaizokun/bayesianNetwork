package test.dynamic;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDouble;
import math.Matrix;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.factory.BatteryDBN;
import network.factory.NetworkFactory;
import org.junit.Test;

import java.util.Map;

import static network.factory.BatteryNetworkFactory.BATTERY_VARS.GAUGE;

public class DBNBatteryTest {

    @Test
    public void testBatteryDBN() {

        NetworkFactory batteryDbn = new BatteryDBN();

        DynamicBayesianNetwork network = batteryDbn.initNetwork();

        int gaugeValues[] = new int[]{5, 5, 5, 5, 5, 4, 4, 4, 0, 0, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1, 0};

        //Variable[] gaugeVars = new Variable[gaugeValues.length];

        IDomain batteryLevelDomain = DomainFactory.getBatteryLevelDOmain();

        for (int i = 0; i < gaugeValues.length; i++) {

            Variable obs = new Variable(GAUGE, batteryLevelDomain);

            obs.setValue(gaugeValues[i]);

            //gaugeVars[i] = obs;
            network.extend(obs);
        }

        for (Map.Entry<Integer, Matrix> forward : network.getForwards()) {

            System.out.print(forward.getKey()+" : ");

            AbstractDouble max = network.getDoubleFactory().getNew(0.0);

            Domain.DomainValue maxDomainValue = null;

            Matrix matrice = forward.getValue();

            for(int row = 0 ; row < matrice.getRowCount(); row ++){

                if(matrice.getValue(row).compareTo(max) >= 0){

                    max = matrice.getValue(row);

                    maxDomainValue = matrice.getRowValue(row);
                }
            }

            System.out.println(maxDomainValue+" "+max);

            System.out.println(forward.getValue());

        }
    }

}
