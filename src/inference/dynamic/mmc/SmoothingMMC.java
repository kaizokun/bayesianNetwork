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

    public Matrix smoothing(int time, Map<Integer, Variable> megaVariablesObs){

        Matrix forward = new ForwardMMC(mmc).forward(time, megaVariablesObs, false);

        Matrix backward = new BackwardMMC(mmc).backward(time, megaVariablesObs, false);

        return forward.multiplyRows(backward).normalize();
    }

    public void smoothing(int timeStart, int timeEnd, Map<Integer, Variable> megaVariablesObs){

        ForwardMMC forward = new ForwardMMC(mmc);

        forward.forward(timeEnd, megaVariablesObs, true);

        BackwardMMC backward = new BackwardMMC(mmc);

        backward.backward(timeStart, megaVariablesObs, true);

        while (timeStart <= timeEnd){

            System.out.println("FORWARD TIME["+timeStart+"]\n"+forward.forwards.get(timeStart));

            System.out.println("BACKWARD TIME["+timeStart+"]\n"+backward.backwards.get(timeStart));

            smoothings.put(timeStart, forward.forwards.get(timeStart).multiplyRows(backward.backwards.get(timeStart)).normalize());

            timeStart ++;
        }
    }

    public Map<Integer, Matrix> getSmoothings() {

        return smoothings;
    }
}
