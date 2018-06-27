package test.dynamic;

import math.Matrix;
import math.Transpose;
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

        for(int i = 0 ; i <  10 ; i ++) {

            network.extend();
        }

        System.out.println(network);

        network.showMegaVarsMatrix();

        Matrix m1 = network.getMatrixState0();

        Matrix m2 = network.getMatrixStates();

        Matrix rs = m2.multiply(new Transpose(m1));

        System.out.println(rs);

    }

}
