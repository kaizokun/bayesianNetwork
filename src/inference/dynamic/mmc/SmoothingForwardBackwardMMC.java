package inference.dynamic.mmc;

import math.Matrix;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class SmoothingForwardBackwardMMC extends SmoothingMMC {

    public SmoothingForwardBackwardMMC(MMC mmc, ForwardMMC forwardMMC, BackwardMMC backwardMMC) {

        super(mmc, forwardMMC, backwardMMC);
    }

    /**
     * load all the forward and backward matrices and multiply them at the end
     */
    public Map<Integer, SmoothingMatrices> smoothing(int timeStart, int timeEnd) {

        Map<Integer, SmoothingMatrices> newSmoothings = new Hashtable<>();

        //System.out.println("SMOOTHING FORWARD BACKWARD ( GREEDY MEMORY)");

        this.forwardMMC.forward(timeEnd, true);

        this.backwardMMC.backward(timeStart, true);

        while (timeStart <= timeEnd) {

            Matrix forwardMatrix = this.forwardMMC.forwards.get(timeStart);

            Matrix backwardMatrix = this.backwardMMC.backwards.get(timeStart);

            Matrix smoothingMatrix = forwardMatrix.multiplyRows(backwardMatrix);

            SmoothingMatrices smoothingMatrices = new SmoothingMatrices(forwardMatrix, backwardMatrix, smoothingMatrix.normalize(), timeStart);

            newSmoothings.put(timeStart, smoothingMatrices);

            timeStart++;
        }

        return newSmoothings;
    }




}
