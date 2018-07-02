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

        //recupere l'observation au temps situé après le backward à incrementer pour obtenir le nouveau
        //si on veut passer d'un backward en 2 à 3, le 2 avait été calculé par rapport aux observations en 3
        //et c'est celles ci qu'il faut extraire
        Variable timeEndMegaObs = mmc.getMegaVariableObs(timeEnd);

        Variable currentMegaObs = mmc.getMegaVariableObs(time);

        Matrix timeEndObsInvert = Matrix.invert(this.mmc.getMatrixObs(timeEndMegaObs));

        Matrix currentObs = this.mmc.getMatrixObs(currentMegaObs);

        Matrix transT = mmc.getMatrixStatesT();

        Matrix trans = mmc.getMatrixStates();
        //multiplication par l'inverse de la matrice observation multiplié par l'inverse de matrice transition
        //au temps du backward precedent, correspond à la partie à diviser ( soit à extraire du backward à incrementer )
        return timeEndObsInvert.multiply( transT )
                //multiplié par : le backward
                .multiply(backward)
                //multiplié par : la matrice transition multiplié par la matrice observation pour le dernier temps
                .multiply( trans.multiply(currentObs) );
    }

    public Matrix decrementBackward(int timeEnd, Matrix backward){
        //récupère la megavariable observation au temps du backward à decrementé
        //la distribution backward en temps timeEnd étant calculé par rapport aux observation aux temps suivant
        Variable backwardTimeMegaObs = mmc.getMegaVariableObs(timeEnd + 1);

        Matrix backwardTimeObsMatrix = mmc.getMatrixObs(backwardTimeMegaObs);

        return mmc.getMatrixStates().multiply(backwardTimeObsMatrix).multiply(backward);
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
