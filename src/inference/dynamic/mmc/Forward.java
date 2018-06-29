package inference.dynamic.mmc;

import domain.Domain;
import domain.data.AbstractDouble;
import math.Matrix;
import math.Transpose;
import network.Variable;
import network.dynamic.MMC;

import java.util.*;

public class Forward {

    protected MMC mmc;

    protected Map<Integer, Matrix> forwards = new Hashtable<>(),
                                   forwardsMax = new Hashtable<>();

    public Forward(MMC mmc) {

        this.mmc = mmc;
    }

    public Matrix forward(int t, Map<Integer, Variable> megaVariablesObs) {

        return forward(t, megaVariablesObs, 0).forward;
    }

    private ForwardRs forward(int t, Map<Integer, Variable> megaVariablesObs, int depth) {

        /*
         * on pourrait faire des megavariables états observations sur des sous ensemble de colVars du reseau
         * et pas forcement sur la totalité et au besoin
         * */

        if (t == 0) {

            Matrix forward = new Transpose(this.mmc.getMatrixState0());

            Matrix forwardMax = new Transpose(this.mmc.getMatrixState0());

            forward.normalize();

            this.forwards.put(t, forward);

            this.forwardsMax.put(t, forwardMax);

            return new ForwardRs(forward, forwardMax);
        }

        Variable megaObs = megaVariablesObs.get(t);

        Matrix obs = this.mmc.getMatrixObs(megaObs);
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

        ForwardRs rs = forward(t - 1, megaVariablesObs, depth + 1);

        Matrix.MultiplyRs multiplyRs = this.mmc.getMatrixStatesT().multiply(rs.forward, rs.max);

        //Matrix sum = this.mmc.getMatrixStatesT().multiply(rs.forward);
        //inutile de faire une multiplication matricielle avec les observations
        //qui plutot que d'etre placé sur une diagonale
        //sont placé ligne par ligne le resultat de la somme étant toujours sur une colonne également
        //on peut multiplier ligne par ligne
        Matrix forward = obs.multiplyRows(multiplyRs.getSum());

        Matrix forwardMax = obs.multiplyRows(multiplyRs.getMax());

        forwardMax.setMaxPrevious(multiplyRs.getMax().getMaxPrevious());

        forward = forward.normalize();

        this.forwards.put(t, forward);

        this.forwardsMax.put(t, forwardMax);

        return new ForwardRs(forward, forwardMax);
    }

    private class ForwardRs {

        private Matrix forward, max;

        public ForwardRs(Matrix forward, Matrix max) {

            this.forward = forward;

            this.max = max;
        }
    }

    public List<List<Domain.DomainValue>> mostLikelyPath(int t) {

        LinkedList<List<Domain.DomainValue>> mostLikelySequence = new LinkedList<>();

        //récupère la matrice forward du dernier temps;
        Matrix tForwardMax = forwardsMax.get(t);

        AbstractDouble max = mmc.getDoubleFactory().getNew(0.0);

        int maxRow = 0;

        for(int row = 0 ; row < tForwardMax.getRowCount() ; row ++){

            if(tForwardMax.getValue(row,0).compareTo(max) > 0){

                max = tForwardMax.getValue(row,0);

                maxRow = row;
            }
        }

        mostLikelySequence.add(tForwardMax.getRowValue(maxRow));

        //récupere la ligne correspondant à la meilleur valeur precedente
        //à partir des valeurs recu en parametre
        int row = tForwardMax.getPreviousForwardMaxValueRow(maxRow);

        t--;

        while (t > 0) {

            tForwardMax = forwardsMax.get(t);

            mostLikelySequence.addFirst(tForwardMax.getRowValue(row));

            row = tForwardMax.getPreviousForwardMaxValueRow(row);

            t--;
        }

        return mostLikelySequence;
    }

    public Map<Integer, Matrix> getForwards() {

        return forwards;
    }
}
