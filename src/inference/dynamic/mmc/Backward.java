package inference.dynamic.mmc;

import domain.data.AbstractDouble;
import math.Matrix;
import network.Variable;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class Backward {

    protected MMC mmc;

    protected Map<Integer, Matrix> backwards = new Hashtable<>();

    public Backward(MMC mmc) {

        this.mmc = mmc;
    }

    public Matrix backward(int t, Map<Integer, Variable> megaVariablesObs) {

        return backward(t, megaVariablesObs, 0);
    }

    private Matrix backward(int t, Map<Integer, Variable> megaVariablesObs, int depth) {

        if(t == this.mmc.getTime()){

            int rows = 1;
            //la matrice limite contient uniquement des valerus à 1
            //et autant de lignes qu'il y a de combinaisons de valeurs pour les variables états
            for( Variable state : this.mmc.getMegaVariableStates1().getCompoVars()){

                rows *= state.getDomainSize();
            }

            //une ligne par valeur
            AbstractDouble[][] limitMatrix = new AbstractDouble[rows][1];

            for(int row = 0 ; row < rows ; row ++ ){

                limitMatrix[row][0] = mmc.getDoubleFactory().getNew(1.0);
            }

            Matrix backward = new Matrix(limitMatrix, mmc.getDoubleFactory());

            backwards.put(t, backward);

            return backward;
        }
        //observation au temps suivant
        Variable megaObs = megaVariablesObs.get(t + 1);

        Matrix obs = this.mmc.getMatrixObs(megaObs);

        Matrix transition = this.mmc.getMatrixStates();

        Matrix backward = this.backward(t + 1, megaVariablesObs, depth + 1);

        backward = transition.multiply(obs).multiply(backward);

        backwards.put(t, backward);

        return backward;
    }

    public Map<Integer, Matrix> getBackwards() {

        return backwards;
    }
}
