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

    public Matrix nextBackward(int backwardTime, int time, Matrix backward) {

        //recupere l'observation en temps correspondant au backward precedent à incrementer pour obtenir le nouveau
        Variable previousMegaObs = mmc.getMegaVariableObs(backwardTime - 1);

        Variable currentMegaObs = mmc.getMegaVariableObs(time);

        Matrix previousObsInvert = Matrix.invert(this.mmc.getMatrixObs(previousMegaObs));

        Matrix currentObs = this.mmc.getMatrixObs(currentMegaObs);

        Matrix transT = mmc.getMatrixStatesT();

        Matrix trans = mmc.getMatrixStates();
        //multiplication par l'inverse de la matrice observation multiplié par l'inverse de matrice transition
        //au temps du backward precedent, correspond à la partie à diviser ( soit à extraire du backward à incrementer )
        return previousObsInvert.multiply( transT )
                //multiplié par : le backward
                .multiply(backward)
                //multiplié par : la matrice transition multiplié par la matrice observation pour le dernier temps
                .multiply( trans.multiply(currentObs) );
    }

    public Matrix backward(int t) {

        return backward(t, 0, false);
    }

    public Matrix backward(int t, boolean saveBackward) {

        return backward(t, 0, saveBackward);
    }

    private Matrix backward(int t, int depth, boolean saveBackward) {

        if(t == this.mmc.getTime()){

            int rows = 1;
            //la matrice limite contient uniquement des valerus à 1
            //et autant de lignes qu'il y a de combinaisons de valeurs pour les colVars états
            for( Variable state : this.mmc.getMegaVariableStates().getCompoVars()){

                rows *= state.getDomainSize();
            }

            //une ligne par valeur
            AbstractDouble[][] limitMatrix = new AbstractDouble[rows][1];

            for(int row = 0 ; row < rows ; row ++ ){

                limitMatrix[row][0] = mmc.getDoubleFactory().getNew(1.0);
            }

            Matrix backward = new Matrix(limitMatrix, mmc.getDoubleFactory());

            if(saveBackward) {

                backwards.put(t, backward);
            }

            return backward;
        }
        //observation au temps suivant
        Variable megaObs = mmc.getMegaVariableObs(t + 1);

        Matrix obs = this.mmc.getMatrixObs(megaObs);

        Matrix transition = this.mmc.getMatrixStates();

        Matrix backward = this.backward(t + 1, depth + 1, saveBackward);

        backward = transition.multiply(obs).multiply(backward);

        if(saveBackward) {

            backwards.put(t, backward);
        }

        return backward;
    }

    public Map<Integer, Matrix> getBackwards() {

        return backwards;
    }


}
