package test.dynamic;

import network.BayesianNetworkFactory;
import network.dynamic.MMC;
import org.junit.Test;

public class MMCtest {

    @Test
    public void megaVariableTestTwoVars() {

    }

    @Test
    public void megaVariableTestOneVar() {

        MMC network = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

        for(int i = 0 ; i <  10 ; i ++)
            network.extend();

        System.out.println(network);

        network.showMegaVarsMatrix();

    }

}
