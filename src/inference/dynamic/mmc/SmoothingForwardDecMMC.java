package inference.dynamic.mmc;

import math.Matrix;
import math.MatrixUtil;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class SmoothingForwardDecMMC extends SmoothingMMC {

    public SmoothingForwardDecMMC(MMC mmc, ForwardMMC forwardMMC, BackwardMMC backwardMMC) {

        super(mmc, forwardMMC, backwardMMC);
    }


    @Override
    public Map<Integer, SmoothingMatrices> smoothing(int timeStart, int timeEnd) {

        //System.out.println("SMOOTHING FORWARD INCREMENTAL (CONSTANT MEMORY)");

        Map<Integer, SmoothingMatrices> newSmoothings = new Hashtable<>();

        Matrix forwardMatrix = forwardMMC.forward(timeEnd, false);

        Matrix backwardMatrix = backwardMMC.backward(timeEnd, false);

        SmoothingMatrices smoothingMatrices = new SmoothingMatrices(forwardMatrix, backwardMatrix, forwardMatrix.multiplyRows(backwardMatrix).normalize(), timeEnd);

        newSmoothings.put(timeEnd, smoothingMatrices);

        this.smoothing(forwardMatrix, backwardMatrix, timeStart, timeEnd - 1, newSmoothings);

        return newSmoothings;
    }

    private void smoothing(Matrix forwardMatrix, Matrix backwardMatrix, int timeStart, int timeEnd, Map<Integer, SmoothingMatrices> smoothings) {

        if (timeStart > timeEnd) {

            return;
        }
        //observation au temps suivant, le backward se calculant par rapport aux observations suivantes
        //récupère la bonne matrice en fonction des valeurs des observations
        Matrix obs = this.mmc.getMatrixObs(timeEnd + 1);
        //récupère la matrice transition
        Matrix transition = this.mmc.getMatrixStates();
        //calcul le backward pour timeEnd courant
        backwardMatrix = MatrixUtil.multiply(MatrixUtil.multiply(transition, obs), backwardMatrix);
        //calcul le forward courant à partir du forward en timeEnd + 1
        //inverse de la transposée de la matrice transition
        Matrix reverseStatesT = MatrixUtil.invert(mmc.getMatrixStatesT());
        //inverse de la matrice observation
        Matrix reverseObs = MatrixUtil.invert(obs);
        //forward decrementé
        forwardMatrix = MatrixUtil.multiply(MatrixUtil.multiply(reverseStatesT, reverseObs), forwardMatrix).normalize();
        //pour obtenir les bonnes valeurs du smoothing il faut soit normaliser le backward puis le smoothing
        //soit aucun des deux ce qui fait des opérations en moins
        SmoothingMatrices smoothingMatrices = new SmoothingMatrices(forwardMatrix, backwardMatrix, forwardMatrix.multiplyRows(backwardMatrix).normalize(), timeEnd);

        smoothings.put(timeEnd, smoothingMatrices);

        this.smoothing(forwardMatrix, backwardMatrix, timeStart, timeEnd - 1, smoothings);
    }
}
