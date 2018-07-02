package inference.dynamic.mmc;

import inference.dynamic.Forward;
import math.Matrix;
import network.Variable;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class SmoothingMMC {

    protected MMC mmc;

    protected Map<Integer, SmoothingMatrices> smoothings = new Hashtable<>();

    public SmoothingMMC(MMC mmc) {

        this.mmc = mmc;
    }

    public Matrix smoothing(int time) {

        Matrix forward = new ForwardMMC(mmc).forward(time, false);

        Matrix backward = new BackwardMMC(mmc).backward(time, false);

        return forward.multiplyRows(backward).normalize();
    }

    public void smoothing(int timeStart, int timeEnd) {

        ForwardMMC forward = new ForwardMMC(mmc);

        forward.forward(timeEnd, true);

        BackwardMMC backward = new BackwardMMC(mmc);

        backward.backward(timeStart, true);

        while (timeStart <= timeEnd) {

            System.out.println("FORWARD TIME[" + timeStart + "]\n" + forward.forwards.get(timeStart));

            //System.out.println("BACKWARD TIME[" + timeStart + "]\n" + backward.backwards.get(timeStart));

            Matrix forwardMatrix = forward.forwards.get(timeStart);

            Matrix backwardMatrix = backward.backwards.get(timeStart);

            Matrix smoothingMatrix = forwardMatrix.multiply(backwardMatrix);

            SmoothingMatrices smoothingMatrices = new SmoothingMatrices(forwardMatrix, backwardMatrix, smoothingMatrix);

            smoothings.put(timeStart, smoothingMatrices);

            timeStart++;
        }
    }

    public void smoothingConstant() {


        //si timeStart == timeEnd on lisse sur un seul état
        //en général on fera le lissage sur une certain nombre d'états compris entre [time - n; time - 1]
        //le range pourrait aussi être [time - n; time - x]
        //il faut donc verifier si 'time - n' est supérieur ou égal à 1 si inférieur il vaut 1
        //ensuite faire de même pour time - x qui cette fois si il est inférieur à 1 on ne fait rien
        //il n'y a rien à lisser...
        //la premier fois que l'on effectue le smoothing on va utiliser smoothingConstant si possible
        //et enregistrer le backward et le foward ainsi que la multiplication des deux, le lissage,
        //donc au début il se peut que en fonction de la plage que l'on souhaite lisser on le fasse pour 0 à 1 états
        //à parti du moment ou on la fait pour 1 état sauvegardé soit le dernier de la plage que l'on a pu obtenir
        //on peut incrementer son forward et son backward pour faire avance le dernier etat de la plage
        //puis les decrementer pour calculer les precedents et obtenir la plage complete mise à jour.

        //par defaut smootEnd est initialisé à 1 et on applique le lissage sur l'état en time - 1
        int timeEnd = mmc.getTime() - mmc.getSmootEnd();
        //on applique le filtrage sur une sequence de longeur [time - smootStart ; [time - smootEnd]
        int timeStart = mmc.getTime() - mmc.getSmootStart();

        if (timeEnd < 1) {
            //enregistre qu'en temps t le lissage ne s'est pas fait
            mmc.getSmootRange().put(mmc.getTime(), 0);

            return;
        }
        //si timestart est inferieur à 1 on commence à 1
        timeStart = timeStart < 1 ? 1 : timeStart;

        //au depart on lissera sur une plage de longeur [1,1] qui pourrait augmenter par la suite
        //on crée une nouvelle Map pour enregistrer la nouvelle plage de smoothing
        Map<Integer, SmoothingMatrices> newSmoothings = new Hashtable<>();

        if (mmc.getSmootRange().get(mmc.getTime() - 1) > 0) {
            //on a effectué un lissage pour la coupe precedente
            //on recupere le dernier état lissé au timeEnd precedent
            SmoothingMatrices smoothingMatrices = mmc.getSmoothings().get(timeEnd - 1);
            //c'est celui ci dont on va incrementer le forward ainsi que le backward
            //pour calculer le lissage de l'état timeEnd

            ForwardMMC forward = new ForwardMMC(mmc);

            BackwardMMC backward = new BackwardMMC(mmc);

            Matrix timeEndForward = forward.nextForward(timeEnd, smoothingMatrices.forward);

            Matrix timeEndBackward = backward.nextBackward(timeEnd, mmc.getTime(), smoothingMatrices.backward);

            SmoothingMatrices newSmoothingMatrices = new SmoothingMatrices(timeEndForward, timeEndBackward, timeEndForward.multiply(timeEndBackward));
            //on seuvegarde le dernier
            newSmoothings.put(timeEnd, newSmoothingMatrices);

            //on initialise le forward et le backward courant
            //maintenant pour chaque time allant de timeEnd - 1 à timeStart
            //on decremente le backward courant
            //on recupere le forward enregistré pour un precedent smoothing à un temps time
            //si il n'existe pas on decremente le forward courant
            //on enregistre le smoothing pour time

        } else {
            //cas de base ou aucun lissage n'a été effectué on va l'appliquer pour la premier fois

            this.smoothingConstant(timeStart, timeEnd, newSmoothings);

            this.mmc.setSmoothings(newSmoothings);
        }

    }

    public void smoothingConstant(int timeStart, int timeEnd) {

        this.smoothingConstant(timeStart, timeEnd, this.smoothings);
    }

    protected void smoothingConstant(int timeStart, int timeEnd, Map<Integer, SmoothingMatrices> smoothings) {

        ForwardMMC forward = new ForwardMMC(mmc);

        BackwardMMC backward = new BackwardMMC(mmc);

        Matrix forwardMatrix = forward.forward(timeEnd, false);

        Matrix backwardMatrix = backward.backward(timeEnd, false);

        SmoothingMatrices smoothingMatrices = new SmoothingMatrices(forwardMatrix, backwardMatrix, forwardMatrix.multiplyRows(backwardMatrix));

        this.smoothings.put(timeEnd, smoothingMatrices);

        this.smoothingConstant(forwardMatrix, backwardMatrix, timeStart, timeEnd - 1, smoothings);
    }

    private void smoothingConstant(Matrix forwardMatrix, Matrix backwardMatrix, int timeStart, int timeEnd, Map<Integer, SmoothingMatrices> smoothings) {

        if (timeStart > timeEnd) {

            return;
        }

        //observation au temps suivant, le backward se calculant par rapport aux observations suivantes
        Variable megaObs = this.mmc.getMegaVariableObs(timeEnd + 1);
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
        //forward decrementé
        forwardMatrix = reverseStatesT.multiply(reverseObs).multiply(forwardMatrix).normalize();

        System.out.println("FORWARD TIME[" + timeEnd + "]\n" + forwardMatrix);
        //pour obtenir les bonnes valeurs du smoothing il faut soit normaliser le backward puis le smoothing
        //soit aucun des deux ce qui fait des opérations en moins
        SmoothingMatrices smoothingMatrices = new SmoothingMatrices(forwardMatrix, backwardMatrix, forwardMatrix.multiplyRows(backwardMatrix).normalize());

        smoothings.put(timeEnd, smoothingMatrices);

        this.smoothingConstant(forwardMatrix, backwardMatrix, timeStart, timeEnd - 1, smoothings);
    }

    public Map<Integer, SmoothingMatrices> getSmoothings() {

        return smoothings;
    }

    public static class SmoothingMatrices {

        protected Matrix forward, backward, smoothing;

        public SmoothingMatrices(Matrix forward, Matrix backward, Matrix smoothing) {
            this.forward = forward;
            this.backward = backward;
            this.smoothing = smoothing;
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
    }
}
