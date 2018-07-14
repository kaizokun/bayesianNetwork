package test.dynamic;

import domain.Domain;
import domain.IDomain;
import inference.dynamic.mmc.*;
import math.Matrix;
import math.MatrixUtil;
import math.Transpose;
import network.factory.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.MMC;
import org.junit.Test;

import java.util.List;

import static domain.DomainFactory.getBooleanDomain;
import static network.factory.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.*;

public class MMCtest {

    protected MMC mmcOne = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

    //protected MMC mmcTwo = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkTwoVars();

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
        System.out.println("EXTEND");
        mmcOne.extend(variablesObsTab);
        System.out.println("EXTEND END");

        MostLikelySequencyMMC mostLikelySequency = new MostLikelySequencyMMC(mmcOne);

        Matrix forward = mostLikelySequency.mostLikelySequency(10);

        //System.out.println(mmcOne);

        //System.out.println(forward);

        List<Domain.DomainValue> mostLilelySequency = mostLikelySequency.mostLikelyPath(obsValues.length);

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

        Matrix smoothing = mmcOne.getSmoothingMMC().smoothing(1);

        System.out.println("Smoothing");

        System.out.println(smoothing);
    }

    Object[][] obsValues = new Object[][]{{1}, {1}, {1}, {1}, {1}, {1}};

    @Test
    public void smoothingForwardBackwardTestOneVar() {

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);

        mmcOne.setSmoothingMMC(new SmoothingForwardBackwardMMC(mmcOne, mmcOne.getForwardMMC(), mmcOne.getBackwardMMC()));

        mmcOne.setSmootRange(6, 1);

        mmcOne.extend(variablesObsTab, true);
    }

    @Test
    public void smoothingForwardIncrementalTestOneVar() {

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);

        mmcOne.setSmoothingMMC(new SmoothingForwardDecMMC(mmcOne, mmcOne.getForwardMMC(), mmcOne.getBackwardMMC()));

        mmcOne.setSmootRange(6, 1);

        mmcOne.extend(variablesObsTab, true);
    }

    @Test
    public void smoothingForwardDecrementalBackwardIncrementalTestOneVar() {

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);

        mmcOne.setSmoothingMMC(new SmoothingForwardDecBackwardIncMMC(mmcOne, mmcOne.getForwardMMC(), mmcOne.getBackwardMMC()));

        mmcOne.setSmootRange(6, 1);

        mmcOne.extend(variablesObsTab, true);
    }

    @Test
    public void multiplicationObsTransTest() {

        Object[][] obsValues = new Object[][]{{1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA}, new IDomain[]{getBooleanDomain()}, obsValues);

        mmcOne.extend(variablesObsTab);

        Matrix matrixObs = mmcOne.getMatrixObs(1);

        Matrix matrixTrans = mmcOne.getMatrixStates();

        System.out.println(matrixObs);

        System.out.println(matrixTrans);

        System.out.println(MatrixUtil.multiply(MatrixUtil.invert(matrixObs), MatrixUtil.invert(matrixTrans)));

        System.out.println(MatrixUtil.invert(MatrixUtil.multiply(matrixObs, matrixTrans)));

        System.out.println(MatrixUtil.invert(MatrixUtil.multiply(matrixTrans, matrixObs)));
    }

    /*
    @Test
    public void forwardAndBackwardTestTwoVar() {

        Object[][] obsValues = new Object[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA, COAT},
                new IDomain[]{getBooleanDomain(), getBooleanDomain()},
                obsValues);

        mmcTwo.extend(variablesObsTab);

        ForwardMMC mmcForward = new ForwardMMC(mmcTwo);

        BackwardMMC mmcBackward = new BackwardMMC(mmcTwo);

        Matrix forward = mmcForward.forward(2);

        Matrix backward = mmcBackward.backward(2);

        System.out.println(forward);

        System.out.println(backward);
    }
*/
    @Test
    public void testForwardMultiplicationOrder() {

        Object[][] obsValues = new Object[][]{{1}, {1}, {1}, {1}, {1}, {1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA},
                new IDomain[]{getBooleanDomain()},
                obsValues);

        mmcOne.extend(variablesObsTab);

        Matrix forward = new Transpose(mmcOne.getMatrixState0());

        Matrix transitionT = mmcOne.getMatrixStatesT();

        System.out.println("TRANSITION T");

        System.out.println(transitionT);

        System.out.println("FOWARD 0");

        System.out.println(forward);

        for (int t = 1; t < obsValues.length; t++) {

            Matrix observation = mmcOne.getMatrixObs(t);

            //forward = observation.multiply(new Transpose(transition)).multiply(forward).normalize();

            forward = MatrixUtil.multiply(observation, MatrixUtil.multiply(transitionT, forward)).normalize();

            System.out.println("FORWARD " + t);

            System.out.println(forward);
        }
    }

    @Test
    public void testBackwardMultiplicationOrder() {

        Object[][] obsValues = new Object[][]{{1}, {1}, {1}, {1}, {1}, {1}};

        Variable[][] variablesObsTab = getVariablesInit(new Object[]{UMBRELLA},
                new IDomain[]{getBooleanDomain()},
                obsValues);

        mmcOne.extend(variablesObsTab);

        Matrix backward = mmcOne.getBackwardInit();

        Matrix transition = mmcOne.getMatrixStates();

        System.out.println("TRANSITION");

        System.out.println(transition);

        System.out.println("Backward " + (obsValues.length + 1));

        System.out.println(backward);

        for (int t = obsValues.length; t >= 1; t--) {

            Matrix observation = mmcOne.getMatrixObs(t);

            backward = MatrixUtil.multiply(transition, MatrixUtil.multiply(mmcOne.getMatrixObs(t), backward));

            //backward = transition.multiply(mmcOne.getMatrixObs(t)).multiply(backward);

            System.out.println("BACKWARD " + t);

            System.out.println(backward);
        }
    }

    @Test
    public void invertMatrixTest() {

        MMC network = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

        Matrix m2 = network.getMatrixStates();

        Matrix m2Invert = MatrixUtil.invert(m2);

        System.out.println(m2);

        System.out.println(m2Invert);

        System.out.println(MatrixUtil.multiply(m2, m2Invert));
    }


}
