package test.dynamic;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import math.Matrix;
import math.Transpose;
import network.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.MMC;
import org.junit.Test;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.*;

public class MMCtest {

    @Test
    public void megaVariableTestTwoVars() {

    }

    @Test
    public void multiplyMatrixTest() {

        MMC network = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

        Matrix m1 = network.getMatrixState0();

        Matrix m2 = network.getMatrixStates();

        Matrix rs = m2.multiply(new Transpose(m1));

        System.out.println(rs);
    }

    @Test
    public void megaVariableTestOneVar() {

        MMC network = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

        for(int i = 1 ; i <=  2 ; i ++) {

            network.extend();
        }

        System.out.println(network);

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        Variable umbrella = new Variable(UMBRELLA.toString());

        int obsValues[][] = new int[][]{{1},{1}};

        int time = 1;

        Map<Integer,Variable> megaVarObs = new Hashtable<>();

        for(int values[] : obsValues) {

            for(int value : values) {

                Variable megaVariableObservation = network.getMegaVariable(time, umbrella);

                megaVariableObservation.setDomainValues(booleanDomain.getDomainValue(value));

                megaVarObs.put(time, megaVariableObservation);
            }

            time ++;
        }

        Matrix forward = network.forward(2, megaVarObs);

        System.out.println(forward);

    }

}
