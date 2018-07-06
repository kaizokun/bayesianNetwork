package inference.dynamic.mmc;

import math.Matrix;
import network.Variable;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public abstract class SmoothingMMC {

    protected MMC mmc;

    protected ForwardMMC forwardMMC;

    protected BackwardMMC backwardMMC;

    //protected Map<Integer, SmoothingMatrices> smoothings = new Hashtable<>();

    public SmoothingMMC(MMC mmc, ForwardMMC forwardMMC, BackwardMMC backwardMMC) {

        this.mmc = mmc;

        this.forwardMMC = forwardMMC;

        this.backwardMMC = backwardMMC;
    }

    public Matrix smoothing(int time) {

        Matrix forward = this.forwardMMC.forward(time, false);

        Matrix backward = this.backwardMMC.backward(time, false);

        return forward.multiplyRows(backward).normalize();
    }

    public void smoothing() {

        //par defaut smootEnd est initialisé à 1 et on applique le lissage sur l'état en time - 1
        int timeEnd = mmc.getTime() - mmc.getSmootEnd();
        //on applique le filtrage sur une sequence de longeur [time - smootStart ; [time - smootEnd]
        int timeStart = mmc.getTime() - mmc.getSmootStart();

        if (timeEnd < 0) {

            return;
        }
        //si timestart est inferieur à 1 on commence à 1
        timeStart = timeStart < 0 ? 0 : timeStart;

        this.mmc.setSmoothings(this.smoothing(timeStart, timeEnd));
    }

    public abstract Map<Integer, SmoothingMatrices> smoothing(int timeStart, int timeEnd);

/*
    public Map<Integer, SmoothingMatrices> getSmoothings() {

        return smoothings;
    }
*/
    public static class SmoothingMatrices {

        protected Matrix forward, backward, smoothing;

        protected int time;

        public SmoothingMatrices(Matrix forward, Matrix backward, Matrix smoothing, int time) {

            this.forward = forward;

            this.backward = backward;

            this.smoothing = smoothing;

            this.time = time;
        }

        public Matrix getForward() {
            return forward;
        }

        public void setForward(Matrix forward) {
            this.forward = forward;
        }

        public Matrix getBackward() {
            return backward;
        }

        public void setBackward(Matrix backward) {
            this.backward = backward;
        }

        public Matrix getSmoothing() {
            return smoothing;
        }

        public void setSmoothing(Matrix smoothing) {
            this.smoothing = smoothing;
        }


        @Override
        public String toString() {

            StringBuilder builder = new StringBuilder();
            /*
            builder.append("=============================\n");
            builder.append("===========FORWARD "+time+"===========\n");
            builder.append("=============================\n");

            builder.append(this.forward.toString());
            builder.append("=============================\n");
            builder.append("===========BACKWARD "+time+"==========\n");
            builder.append("=============================\n");
            builder.append(this.backward.toString());
            */
            builder.append("===================================\n");
            builder.append("===========SMOOTHING [" + time + "]=========\n");
            builder.append("===================================\n");

            builder.append(this.smoothing.toString());

            return builder.toString();
        }
    }
}
