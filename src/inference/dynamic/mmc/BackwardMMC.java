package inference.dynamic.mmc;

import domain.data.AbstractDouble;
import math.Matrix;
import math.MatrixDiagonal;
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

        //recupere l'observation au temps situé après le backward à incrementer pour obtenir le nouveau
        //si on veut passer d'un backward en 2 à 3, le 2 avait été calculé par rapport aux observations en 3
        //et c'est celles ci qu'il faut extraire
        Matrix timeEndObs = this.mmc.getMatrixObs(timeEnd);

        MatrixDiagonal currentObs = (MatrixDiagonal) this.mmc.getMatrixObs(time);

        Matrix trans = mmc.getMatrixStates();

        //partie à retirer ou à diviser ( ou multiplier par une matrice inverse )
        //multiplier les inverses ou inverser la multiplication en changeant l'ordre des matrice donne le meme resultat
        //on ne fait qu'une inversion dans le deuxieme cas ...

        Matrix denom = Matrix.multiply(Matrix.invert(timeEndObs), Matrix.invert(trans));

       // Matrix denom = Matrix.invert(Matrix.multiply(trans, timeEndObs));
        //partie à ajouter ou à multiplier
       // Matrix num = Matrix.multiply(trans, currentObs);
        //on commence par multiplier

        //Matrix newBackward = Matrix.multiply(num, Matrix.multiply(denom, backward));
        Matrix newBackward = Matrix.multiply(trans, currentObs.multiplyRows(Matrix.multiply(denom, backward)));
/*
        System.out.println("OBS INVERT");
        System.out.println(Matrix.invert(timeEndObs));

        System.out.println("OBS INVERT x OBS");
        System.out.println(Matrix.multiply(Matrix.invert(timeEndObs), timeEndObs));
*/
/*
        System.out.println("TRANS ");
        System.out.println(trans);

        System.out.println("TRANS INVERT");
        System.out.println(Matrix.invert(trans));

        System.out.println("TRANS * OBS");
        System.out.println(Matrix.multiply(trans, timeEndObs));
*/
/*
        System.out.println("DENOM");
        System.out.println(denom);
        System.out.println("BACKWARD");
        System.out.println(backward);
        System.out.println("DENOM * BACKWARD");*/
       // System.out.println(Matrix.multiply(denom, backward));

   // if(time == 3)
       // System.exit(0);
/*
        Matrix newBackward = Matrix.invert(timeEndObs).multiplyRows( // 1 colonne
                Matrix.multiply(Matrix.invert(trans), backward). // 1 colonne
                        multiplyRows(Matrix.multiply(trans, currentObs.getColMatrix()))); //1 colonne
*/
        return newBackward;
    }

    public Matrix decrementBackward(int timeEnd, Matrix backward) {
        //récupère la megavariable observation au temps du backward à decrementé
        //la distribution backward en temps timeEnd étant calculé par rapport aux observation aux temps suivant

        Matrix matrixObs = mmc.getMatrixObs(timeEnd + 1);

        return Matrix.multiply(mmc.getMatrixStates(), matrixObs.multiplyRows(backward));
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

        backward = Matrix.multiply(transition, obs.multiplyRows(backward));

        if (saveBackward) {

            backwards.put(t, backward);
        }

        return backward;
    }

    public Map<Integer, Matrix> getBackwards() {

        return backwards;
    }


}
