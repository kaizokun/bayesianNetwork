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

import static network.BayesianNetworkFactory.UMBRELLA_NETWORK_VARS.*;

public class MMCtest {

    protected MMC mmc;

    protected Map<Integer, Variable> megaVariableTestTwoVars(int extend, int[][] obsValues) {

        mmc = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkTwoVars();
        //le MMC possede déja une megavariable etendu au temps 1
        for (int i = 2; i <= extend; i++) {

            mmc.extend();
        }

        IDomain booleanDomain = DomainFactory.getBooleanDomain();

        Variable umbrella = new Variable(UMBRELLA.toString(), booleanDomain);

        Variable coat = new Variable(COAT.toString(), booleanDomain);

        int time = 1;

        Map<Integer, Variable> megaVarObs = new Hashtable<>();

        for (int values[] : obsValues) {

            Variable megaVariableObservation = mmc.getMegaVariable(time, umbrella, coat);

            umbrella.setValue(values[0]);

            coat.setValue(values[1]);

            megaVariableObservation.setDomainValuesFromVariables(umbrella, coat);

            megaVarObs.put(time, megaVariableObservation);

            time++;
        }

        return megaVarObs;
    }

    protected Map<Integer, Variable> megaVariableTestOneVar(int extend, int[][] obsValues) {

        mmc = BayesianNetworkFactory.getUmbrellaMMCDynamicNetworkOneVars();

        for (int i = 1; i <= extend; i++) {

            mmc.extend();
        }

        Variable umbrella = new Variable(UMBRELLA.toString(), DomainFactory.getBooleanDomain());

        int time = 1;

        Map<Integer, Variable> megaVarObs = new Hashtable<>();

        for (int values[] : obsValues) {

            Variable megaVariableObservation = mmc.getMegaVariable(time, umbrella);

            umbrella.setValue(values[0]);

            megaVariableObservation.setDomainValuesFromVariables(umbrella);

            megaVarObs.put(time, megaVariableObservation);

            time++;
        }

        return megaVarObs;
    }

    @Test
    public void forwardTestOneVar() {

        int[][] obsValues = new int[][]{{1}, {1}};

        Map<Integer, Variable> megaVarObs = megaVariableTestOneVar(obsValues.length - 1, obsValues);

        ForwardMMC mmcForward = new ForwardMMC(mmc);

        Matrix forward = mmcForward.forward(2, megaVarObs);

        System.out.println(mmc);

        System.out.println(forward);
    }

    @Test
    public void mostLikelyPathTestOneVar() {

        int[][] obsValues = new int[][]{{1}, {1}, {1}, {0}, {0}, {1}, {0}, {1}, {0}, {1}};

        Map<Integer, Variable> megaVarObs = megaVariableTestOneVar(obsValues.length - 1, obsValues);

        MostLikelySequencyMMC mostLikelySequency = new MostLikelySequencyMMC(mmc);

        Matrix forward = mostLikelySequency.forward(10, megaVarObs);

        System.out.println(mmc);

        System.out.println(forward);

        List<List<Domain.DomainValue>> mostLilelySequency = mostLikelySequency.mostLikelyPath(obsValues.length);

        System.out.println(mostLilelySequency);
    }

    @Test
    public void BackwardTestOneVar() {

        int[][] obsValues = new int[][]{{1}, {1}};

        Map<Integer, Variable> megaVarObs = megaVariableTestOneVar(obsValues.length - 1, obsValues);

        BackwardMMC mmcBackward = new BackwardMMC(mmc);

        Matrix backward = mmcBackward.backward(1, megaVarObs);

        System.out.println(mmc);

        System.out.println(backward);
    }

    @Test
    public void smoothingTestOneVar() {

        int[][] obsValues = new int[][]{{1}, {1}};

        Map<Integer, Variable> megaVarObs = megaVariableTestOneVar(obsValues.length - 1, obsValues);

        System.out.println(mmc);

        Matrix smoothing = new SmoothingMMC(mmc).smoothing(1, megaVarObs);

        System.out.println("Smoothing");

        System.out.println(smoothing);
    }

    @Test
    public void smoothingRangeTestOneVar() {

        int[][] obsValues = new int[][]{{1}, {1}, {1}, {1}, {1}, {1}};

        Map<Integer, Variable> megaVarObs = megaVariableTestOneVar(obsValues.length - 1, obsValues);

        // System.out.println(mmc);

        SmoothingMMC smoothing = new SmoothingMMC(mmc);

        smoothing.smoothing(0, 6, megaVarObs);

        System.out.println("Smoothings");

        System.out.println(smoothing.getSmoothings());
    }


    @Test
    public void smoothingConstantRangeTestOneVar() {

        int[][] obsValues = new int[][]{{1}, {1}, {1}, {1}, {1}, {1}};

        Map<Integer, Variable> megaVarObs = megaVariableTestOneVar(obsValues.length - 1, obsValues);

        // System.out.println(mmc);

        SmoothingMMC smoothing = new SmoothingMMC(mmc);

        smoothing.smoothingConstant(0, 6, megaVarObs);

        System.out.println("Smoothings");

        System.out.println(smoothing.getSmoothings());
    }

    @Test
    public void forwardBackwardTestTwoVar() {

        int[][] obsValues = new int[][]{{1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}, {1, 1}};

        Map<Integer, Variable> megaVarObs = megaVariableTestTwoVars(obsValues.length - 1, obsValues);

        ForwardMMC mmcForward = new ForwardMMC(mmc);

        BackwardMMC mmcBackward = new BackwardMMC(mmc);

        Matrix forward = mmcForward.forward(2, megaVarObs);

        Matrix backward = mmcBackward.backward(2, megaVarObs);

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
