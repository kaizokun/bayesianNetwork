package inference.dynamic.mmc;

import math.Matrix;
import network.Variable;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class SmoothingMMC {

    protected MMC mmc;

    protected Map<Integer, Matrix> smoothings = new Hashtable<>();

    public SmoothingMMC(MMC mmc) {

        this.mmc = mmc;
    }

    public Matrix smoothing(int time, Map<Integer, Variable> megaVariablesObs) {

        Matrix forward = new ForwardMMC(mmc).forward(time, megaVariablesObs, false);

        Matrix backward = new BackwardMMC(mmc).backward(time, megaVariablesObs, false);

        return forward.multiplyRows(backward).normalize();
    }

    public void smoothingConstant(int timeStart, int timeEnd, Map<Integer, Variable> megaVariablesObs) {

        ForwardMMC forward = new ForwardMMC(mmc);

        BackwardMMC backward = new BackwardMMC(mmc);

        Matrix forwardMatrix = forward.forward(timeEnd, megaVariablesObs, false);

        Matrix backwardMatrix = backward.backward(timeEnd, megaVariablesObs, false);

        this.smoothings.put(timeEnd, forwardMatrix.multiplyRows(backwardMatrix));

        this.smoothing(forwardMatrix, backwardMatrix, timeStart, timeEnd - 1, megaVariablesObs);
    }

    private void smoothing(Matrix forwardMatrix, Matrix backwardMatrix, int timeStart, int timeEnd, Map<Integer, Variable> megaVariablesObs) {

        if (timeStart > timeEnd){

            return;
        }

        //observation au temps suivant, le backward se calculant par rapport aux observations suivantes
        Variable megaObs = megaVariablesObs.get(timeEnd + 1);
        //récupère la bonne matrice en fonction des valeurs des observations
        Matrix obs = this.mmc.getMatrixObs(megaObs);
        //récupère la matrice transition
        Matrix transition = this.mmc.getMatrixStates();
        //calcul le backward pour timeEnd courant
        backwardMatrix = transition.multiply(obs).multiply(backwardMatrix);
        //calcul le forward courant à partir du forward en timeEnd + 1
        //inverse de la transposée de la matrice transition
        Matrix reverseStatesT = Matrix.invert(mmc.getMatrixStatesT());
        //inverse de la matrice observation
        Matrix reverseObs = Matrix.invert(obs);

        forwardMatrix = reverseStatesT.multiply(reverseObs).multiply(forwardMatrix).normalize();

        System.out.println("FORWARD TIME[" + timeEnd + "]\n" + forwardMatrix);

        this.smoothings.put(timeEnd, forwardMatrix.multiplyRows(backwardMatrix));

        this.smoothing(forwardMatrix, backwardMatrix, timeStart, timeEnd - 1, megaVariablesObs);
    }

    public void smoothing(int timeStart, int timeEnd, Map<Integer, Variable> megaVariablesObs) {

        ForwardMMC forward = new ForwardMMC(mmc);

        forward.forward(timeEnd, megaVariablesObs, true);

        BackwardMMC backward = new BackwardMMC(mmc);

        backward.backward(timeStart, megaVariablesObs, true);

        while (timeStart <= timeEnd) {

            System.out.println("FORWARD TIME[" + timeStart + "]\n" + forward.forwards.get(timeStart));

            //System.out.println("BACKWARD TIME[" + timeStart + "]\n" + backward.backwards.get(timeStart));

            smoothings.put(timeStart, forward.forwards.get(timeStart).multiplyRows(backward.backwards.get(timeStart)).normalize());

            timeStart++;
        }
    }

    public Map<Integer, Matrix> getSmoothings() {

        return smoothings;
    }
}
