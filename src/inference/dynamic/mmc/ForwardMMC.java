package inference.dynamic.mmc;

import domain.Domain;
import math.Matrix;
import math.MatrixUtil;
import network.dynamic.MMC;

import java.util.*;

import static math.MatrixUtil.invert;

public class ForwardMMC implements IForwardMMC {

    protected MMC mmc;

    protected Map<Integer, Matrix> forwards = new Hashtable<>();

    public ForwardMMC(MMC mmc) {

        this.mmc = mmc;
    }

    @Override
    public Matrix forward() {

        return this.forward(false);
    }

    protected Map.Entry<Integer, Matrix> getLastDistribution(){

        return mmc.getLastForward();
    }

    @Override
    public Matrix forward(boolean saveForward) {

        //récupère le dernier forward ou max enregistré
        Map.Entry<Integer, Matrix> lastDistrib = getLastDistribution();

        Matrix forward;
        //si aucun forward n'est enregistré ou que son temps ne correspond pas au temps precedent
        if (lastDistrib == null || !lastDistrib.getKey().equals(mmc.getTime() - 1)) {
            //calcul un nouveau forward
            forward = forward(mmc.getTime(), 0, saveForward);
        } else {
            //sinon incremente le forward precedent
            forward = incrementForward(mmc.getTime(), lastDistrib.getValue(), saveForward);
        }

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

    protected Matrix incrementForward(int timeEnd, Matrix lastForward, boolean saveForward) {

        Matrix sum = this.multiplyTransitionForward(this.mmc.getMatrixStatesT(), lastForward);

        Matrix forward = this.mmc.getMatrixObs(timeEnd).multiplyRows(sum);

        forward.normalize();

        mostLikelyPath(forward, sum);

        if (saveForward) {

            this.forwards.put(timeEnd, forward);
        }

        return forward;
    }

    public Matrix decrementForward(int time, Matrix timeEndForward) {

        //récupère l'observation au temps du forward à decrementer
        Matrix matriceObservation = this.mmc.getMatrixObs(time + 1);
        //divise le forward par la matrice observation au temps suivant ainsi que la transition
        //pour retrouver l'état precedent
        //inverse de la transition x ( inverse observation x forward )
        return MatrixUtil.multiply(invert(mmc.getMatrixStatesT()), invert(matriceObservation).multiplyRows(timeEndForward)).normalize();
    }

    private Matrix forward(int t, int depth, boolean saveForwards) {

        // on pourrait faire des megavariables états observations sur des sous ensemble de colVars du reseau
        // et pas forcement sur la totalité et au besoin

        if (t == mmc.getInitTime()) {
            //encapsule le vecteur de base qui est horyzontal dans une transposée pour obtenir un vecteur vertical

            if (saveForwards) {

                this.forwards.put(t, this.mmc.getMatrixState0());
            }

            return this.mmc.getMatrixState0();
        }

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

        //inutile de faire une multiplication matricielle avec les observations
        //qui plutot que d'etre placé sur une diagonale
        //sont placé ligne par ligne le resultat de la somme étant toujours sur une colonne également
        //on peut multiplier ligne par ligne
        //cependant la forme carré reste necesaire pour le backward
        //il faudrait deux formes de matrices pour les observations si on veut optimiser un peu.
        //opération supllémentaire pour le most likely path

        forward = incrementForward(t, forward, saveForwards);

        return forward;
    }


    public Matrix mostLikelySequency(int t) {

        throw new UnsupportedOperationException();
    }

    public List<Domain.DomainValue> mostLikelyPath(int t) {

        throw new UnsupportedOperationException();
    }

    protected void mostLikelyPath(Matrix forward, Matrix sum) {
    }

    protected Matrix multiplyTransitionForward(Matrix matrixStatesT, Matrix forward) {

        return MatrixUtil.multiply(this.mmc.getMatrixStatesT(), forward);
    }

    public Map<Integer, Matrix> getForwards() {

        return forwards;
    }

}
