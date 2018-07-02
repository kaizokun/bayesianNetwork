package test.dynamic;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import inference.dynamic.mmc.BackwardMMC;
import inference.dynamic.mmc.ForwardMMC;
import inference.dynamic.mmc.MostLikelySequencyMMC;
import inference.dynamic.mmc.SmoothingMMC;
import math.Matrix;
import math.Transpose;
import network.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.MMC;
import org.junit.Test;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import static domain.DomainFactory.getBooleanDomain;
import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.*;

public class MMCtest {

    protected MMC mmcOne = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

    protected MMC mmcTwo = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkTwoVars();

    private Variable[][] getVariablesInit(Object[] varLabels, IDomain[] varDomains, Object[][] valuesTab) {
        //crée un tableau à deux dimensions, la premier pour le nombre de megavariables d'oservations
        //qui devront être initialisées, la deuxieme pour le nombre de variables qui consitue la megavariable observation

        Variable[][] variablesTab = new Variable[valuesTab.length][valuesTab[0].length];

        int o = 0;
        //pour chaque liste de valeurs prises par les variables composants la mégavariable d'observation
        for (Object[] values : valuesTab) {

            int v = 0;
            //pour chaque valeur
            for (Object value : values) {
                //crée une variable avec un label et un domain donné
                Variable var = new Variable(varLabels[v].toString(), varDomains[v]);
                //initialise la valeur
                var.setValue(value);
                //enregistre la variable
                variablesTab[o][v] = var;

                v++;
            }

            o++;
        }

        return variablesTab;
    }

    @Test
    public void forwardTestOneVar() {

        Object[][] obsValues = new Object[][]{{1}, {1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);
/*
        Variable[][] variablesObsTab = new Variable[2][1];/
        variablesObsTab[0][0] = new Variable(UMBRELLA.toString(), getBooleanDomain(), 1);
        variablesObsTab[1][0] = new Variable(UMBRELLA.toString(), getBooleanDomain(), 1);
*/
        mmcOne.extend(variablesObsTab);

        ForwardMMC mmcForward = new ForwardMMC(mmcOne);

        Matrix forward = mmcForward.forward(2);

        System.out.println(mmcOne);

        System.out.println(forward);
    }

    @Test
    public void mostLikelyPathTestOneVar() {

        Object[][] obsValues = new Object[][]{{1}, {1}, {1}, {0}, {0}, {1}, {0}, {1}, {0}, {1}};

        IDomain domain = getBooleanDomain();

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{domain}, obsValues);

        mmcOne.extend(variablesObsTab);

        MostLikelySequencyMMC mostLikelySequency = new MostLikelySequencyMMC(mmcOne);

        Matrix forward = mostLikelySequency.mostLikelySequency(10);

        System.out.println(mmcOne);

        System.out.println(forward);

        List<List<Domain.DomainValue>> mostLilelySequency = mostLikelySequency.mostLikelyPath(obsValues.length);

        System.out.println(mostLilelySequency);
    }

    @Test
    public void BackwardTestOneVar() {

        Object[][] obsValues = new Object[][]{{1}, {1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);

        mmcOne.extend(variablesObsTab);

        BackwardMMC mmcBackward = new BackwardMMC(mmcOne);

        Matrix backward = mmcBackward.backward(1);

        System.out.println(mmcOne);

        System.out.println(backward);
    }

    @Test
    public void smoothingTestOneVar() {

        Object[][] obsValues = new Object[][]{{1}, {1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);

        mmcOne.extend(variablesObsTab);

        System.out.println(mmcOne);

        Matrix smoothing = new SmoothingMMC(mmcOne).smoothing(1);

        System.out.println("Smoothing");

        System.out.println(smoothing);
    }

    @Test
    public void smoothingRangeTestOneVar() {

        Object[][] obsValues = new Object[][]{{1}, {1}, {1}, {1}, {1}, {1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);

        mmcOne.extend(variablesObsTab);

        // System.out.println(mmc);

        SmoothingMMC smoothing = new SmoothingMMC(mmcOne);

        smoothing.smoothing(0, 6);

        System.out.println("=====================================================");
        System.out.println("====================Smoothings=======================");
        System.out.println("=====================================================");

        System.out.println(smoothing.getSmoothings());
    }


    @Test
    public void smoothingConstantRangeTestOneVar() {

        Object[][] obsValues = new Object[][]{{1}, {1}, {1}, {1}, {1}, {1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);

        mmcOne.extend(variablesObsTab);

        // System.out.println(mmc);

        SmoothingMMC smoothing = new SmoothingMMC(mmcOne);

        smoothing.smoothingConstant(0, 6);

        System.out.println("=====================================================");
        System.out.println("====================Smoothings=======================");
        System.out.println("=====================================================");

        System.out.println(smoothing.getSmoothings());
    }

    @Test
    public void forwardAndBackwardTestTwoVar() {

        Object[][] obsValues = new Object[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA, COAT},
                new IDomain[]{getBooleanDomain(),getBooleanDomain()},
                obsValues);

        mmcTwo.extend(variablesObsTab);

        ForwardMMC mmcForward = new ForwardMMC(mmcTwo);

        BackwardMMC mmcBackward = new BackwardMMC(mmcTwo);

        Matrix forward = mmcForward.forward(2);

        Matrix backward = mmcBackward.backward(2);

        System.out.println(forward);

        System.out.println(backward);
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
    public void invertMatrixTest() {

        MMC network = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

        Matrix m2 = network.getMatrixStates();

        Matrix m2Invert = Matrix.invert(m2);

        System.out.println(m2);

        System.out.println(m2Invert);

        System.out.println(m2.multiply(m2Invert));
    }

}
