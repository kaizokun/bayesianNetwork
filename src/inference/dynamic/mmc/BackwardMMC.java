package inference.dynamic.mmc;

import domain.data.AbstractDouble;
import math.Matrix;
import network.Variable;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class BackwardMMC {

    protected MMC mmc;

    protected Map<Integer, Matrix> backwards = new Hashtable<>();

    public BackwardMMC(MMC mmc) {

        this.mmc = mmc;
    }

    public Matrix incrementBackward(int timeEnd, int time, Matrix backward) {

        // System.out.println("incrementBackward "+timeEnd+" "+time);

        //recupere l'observation au temps situé après le backward à incrementer pour obtenir le nouveau
        //si on veut passer d'un backward en 2 à 3, le 2 avait été calculé par rapport aux observations en 3
        //et c'est celles ci qu'il faut extraire

        Matrix timeEndObs = this.mmc.getMatrixObs(timeEnd);

        Matrix currentObs = this.mmc.getMatrixObs(time);

        Matrix trans = mmc.getMatrixStates();
        //partie à retirer ou à diviser ( ou multiplier par une matrice inverse )
        //multiplier les inverses ou inverser la multiplication en changeant l'ordre des matrice donne le meme resultat
        //on ne fait qu'une inversion dans le deuxieme cas ...
        //Matrix denom = Matrix.invert(timeEndObs).multiply(Matrix.invert(trans));
        Matrix denom = Matrix.invert(trans.multiply(timeEndObs));

        //partie à ajouter ou à multiplier
        Matrix num = trans.multiply(currentObs);
        //on commence par multiplier
        Matrix newBackward = num.multiply(denom.multiply(backward));

        return newBackward;
    }

    public Matrix decrementBackward(int timeEnd, Matrix backward) {
        //récupère la megavariable observation au temps du backward à decrementé
        //la distribution backward en temps timeEnd étant calculé par rapport aux observation aux temps suivant

        Matrix backwardTimeObsMatrix = mmc.getMatrixObs(timeEnd + 1);

        return mmc.getMatrixStates().multiply(backwardTimeObsMatrix).multiply(backward);
    }

    public Matrix backward(int t) {

        return backward(t, 0, false);
    }

    public Matrix backward(int t, boolean saveBackward) {

        return backward(t, 0, saveBackward);
    }

    private Matrix backward(int t, int depth, boolean saveBackward) {

        if (t == this.mmc.getTime()) {

            if (saveBackward) {

                backwards.put(t, mmc.getBackwardInit());
            }

            return mmc.getBackwardInit();
        }
        //observation au temps suivant
        Matrix obs = this.mmc.getMatrixObs(t + 1);

        Matrix transition = this.mmc.getMatrixStates();

        Matrix backward = this.backward(t + 1, depth + 1, saveBackward);

        backward = transition.multiply(obs).multiply(backward);

        if (saveBackward) {

            backwards.put(t, backward);
        }

        return backward;
    }

    public Map<Integer, Matrix> getBackwards() {

        return backwards;
    }


}
