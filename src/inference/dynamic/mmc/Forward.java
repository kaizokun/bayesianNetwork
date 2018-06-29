package inference.dynamic.mmc;

import math.Matrix;
import math.Transpose;
import network.Variable;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class Forward {

    protected MMC mmc;

    protected Map<Integer, Matrix> forwards = new Hashtable<>();

    public Forward(MMC mmc) {

        this.mmc = mmc;
    }

    public Matrix forward(int t, Map<Integer, Variable> megaVariablesObs) {

        return forward(t, megaVariablesObs, 0);
    }

    private Matrix forward(int t, Map<Integer, Variable> megaVariablesObs, int depth) {

        /*
         * on pourrait faire des megavariables états observations sur des sous ensemble de variables du reseau
         * et pas forcement sur la totalité et au besoin
         *
         * */

        if (t == 0) {

            Matrix forward = new Transpose(this.mmc.getMatrixState0());

            forward.normalize();

            forwards.put(t, forward);

            return forward;
        }

        Variable megaObs = megaVariablesObs.get(t);

        Matrix obs = this.mmc.getMatrixObs(megaObs);
        //dans la matrice de base les lignes correspondent aux valeurs parents
        //et les colones aux valeurs enfants, dans la transposée c'est l'inverse.
        //la multiplication matricielle se fait ligne par ligne pour la transposée de la premiere matrice
        //soit valeur par valeur de la megavariable états
        //puis pour chaque ligne la somme se fait colonne par colonne
        //soit pour chaque valeur prise par la megavariable parents située au temps précédent
        //la matrice resultante contient une ligne par  valeur de la megavariable (enfant) en temps t
        //la matrice observation contient une valeur par ligne pour chaque valeur de la la megavariable en temps t
        //ces valeurs sont sur la diagonale le reste à zero pour faciliter le calcul
        //en multipliant la matrice somme par la matrice observation on obtient la distribution forward
        //sur les valeurs de la megavariable en temps t ligne par ligne
        Matrix sum = this.mmc.getMatrixStatesT().multiply(forward(t - 1, megaVariablesObs, depth + 1));

        Matrix forward = obs.multiply(sum);

        forward = forward.normalize();

        forwards.put(t, forward);

        return forward;
    }

    public Map<Integer, Matrix> getForwards() {

        return forwards;
    }
}
