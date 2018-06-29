package inference.dynamic.mmc;

import math.Matrix;
import network.Variable;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class Smoothing {

    protected MMC mmc;

    protected Map<Integer, Matrix> smoothings = new Hashtable<>();

    public Smoothing(MMC mmc) {

        this.mmc = mmc;
    }

    public Matrix smoothing(int time, Map<Integer, Variable> megaVariablesObs){

        Matrix forward = new Forward(mmc).forward(time, megaVariablesObs);

        Matrix backward = new Backward(mmc).backward(time, megaVariablesObs);

        return forward.multiplyRows(backward).normalize();
    }

    public void smoothing(int timeStart, int timeEnd, Map<Integer, Variable> megaVariablesObs){

        Forward forward = new Forward(mmc);

        forward.forward(timeEnd, megaVariablesObs);

        Backward backward = new Backward(mmc);

        backward.backward(timeStart, megaVariablesObs);

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
