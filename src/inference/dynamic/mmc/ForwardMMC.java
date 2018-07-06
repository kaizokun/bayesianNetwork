package inference.dynamic.mmc;

import inference.dynamic.Util;
import math.Matrix;
import math.Transpose;
import network.dynamic.MMC;

import java.util.*;

public class ForwardMMC implements IForward {

    protected MMC mmc;

    protected Map<Integer, Matrix> forwards = new Hashtable<>();

    public ForwardMMC(MMC mmc) {

        this.mmc = mmc;
    }

    @Override
    public Matrix forward() {

        //récupère le dernier forward enregistré
        Map.Entry<Integer, Matrix> lastForward = mmc.getLastForward();

        Matrix forward;
        //si aucun forward n'est enregistré ou que son temps ne correspond pas au temps precedent
        if (lastForward == null || !lastForward.getKey().equals(mmc.getTime() - 1)) {
            //calcul un nouveau forward
            forward = forward(mmc.getTime(), 0, false);
        }else{
            //sinon incremente le forward precedent
            forward = incrementForward(mmc.getTime(), lastForward.getValue());
        }

        lastForward = new AbstractMap.SimpleEntry(mmc.getTime(), forward);

        mmc.setLastForward(lastForward);

        return forward;
    }

    @Override
    public Matrix forward(int t) {

        return forward(t, 0, false);
    }

    @Override
    public Matrix forward(int t, boolean saveForwards) {

        return forward(t, 0, saveForwards);
    }

    protected Matrix incrementForward(int timeEnd, Matrix lastForward) {

        Matrix obs = this.mmc.getMatrixObs(timeEnd);

        Matrix transT = this.mmc.getMatrixStatesT();

        return obs.multiplyRows(Matrix.multiply(transT, lastForward)).normalize();
    }

    public Matrix decrementForward(int timeEnd, Matrix timeEndForward) {

        //récupère l'observation au temps du forward à decrementer
        Matrix matriceObservation = this.mmc.getMatrixObs(timeEnd + 1);
        //divise le forward par la matrice observation au temps suivant ainsi que la transition
        //pour retrouver l'état precedent
        //inverse de la transition x ( inverse observation x forward )
        return Matrix.multiply(Matrix.invert(mmc.getMatrixStatesT()), Matrix.invert(matriceObservation).multiplyRows(timeEndForward)).normalize();
    }

    private Matrix forward(int t, int depth, boolean saveForwards) {

        /*
         * on pourrait faire des megavariables états observations sur des sous ensemble de colVars du reseau
         * et pas forcement sur la totalité et au besoin
         * */

        if (t == 0) {
            //encapsule le vecteur de base qui est horyzontal dans une transposée pour obtenir un vecteur vertical

            if (saveForwards) {

                this.forwards.put(t, this.mmc.getMatrixState0());
            }

            return this.mmc.getMatrixState0();
        }

        Matrix obs = this.mmc.getMatrixObs(t);

        //dans la matrice de base les lignes correspondent aux valeurs parents
        //et les colones aux valeurs enfants, dans la transposée c'est l'inverse.
        //la multiplication matricielle se fait ligne par ligne pour la transposée de la premiere matrice
        //soit valeur par valeur de la megavariable états de l'instant t courant
        //puis pour chaque ligne la somme se fait colonne par colonne
        //soit pour chaque valeur prise par la megavariable parents située au temps précédent
        //la matrice resultante contient une ligne par  valeur de la megavariable (enfant) en temps t
        //la matrice observation contient une valeur par ligne pour chaque valeur de la la megavariable en temps t
        //ces valeurs sont sur la diagonale le reste à zero pour faciliter le calcul
        //en multipliant la matrice somme par la matrice observation on obtient la distribution forward
        //sur les valeurs de la megavariable en temps t ligne par ligne

        //pour recuperer la sequence la plus probable, ici la multiplication se fait depuis la
        //matrice transition pour chaque ligne on calcule une valeur max Xt-1 pour une valeur de Xt

        Matrix forward = forward(t - 1, depth + 1, saveForwards);

        Matrix sum = this.multiplyTransitionForward(this.mmc.getMatrixStatesT(), forward);

        //Matrix sum = this.mmc.getMatrixStatesT().multiply(rs.forward);
        //inutile de faire une multiplication matricielle avec les observations
        //qui plutot que d'etre placé sur une diagonale
        //sont placé ligne par ligne le resultat de la somme étant toujours sur une colonne également
        //on peut multiplier ligne par ligne
        //cependant la forme carré reste necesaire pour le backward
        //il faudrait deux formes de matrices pour les observations si on veut optimiser un peu.
        forward = obs.multiplyRows(sum);

        forward = forward.normalize();
        //opération supllémentaire pour le most likely path
        this.mostLikelyPath(forward, sum);

        if (saveForwards) {

            this.forwards.put(t, forward);
        }

        return forward;
    }

    protected void mostLikelyPath(Matrix forward, Matrix sum) {

    }

    protected Matrix multiplyTransitionForward(Matrix matrixStatesT, Matrix forward) {

        return Matrix.multiply(this.mmc.getMatrixStatesT(), forward);
    }

    public Map<Integer, Matrix> getForwards() {

        return forwards;
    }

}
