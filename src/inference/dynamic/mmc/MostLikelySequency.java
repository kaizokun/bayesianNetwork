package inference.dynamic.mmc;

import domain.Domain;
import domain.data.AbstractDouble;
import math.Matrix;
import network.dynamic.MMC;

import java.util.LinkedList;
import java.util.List;

public class MostLikelySequency extends Forward{


    public MostLikelySequency(MMC mmc) {

        super(mmc);
    }

    @Override
    protected Matrix multiplyTransitionForward(Matrix matrixStatesT, Matrix forward) {
        //multiplication qui differe de la multiplication habituelle
        //ou plutot que de sommer sur chaque valeur état parent on sauvegarde le maximum
        return matrixStatesT.multiplyMax(forward);
    }

    @Override
    protected void mostLikelyPath(Matrix forward, Matrix sum) {
        //enregsitre pour chaque indice de valeur de la distribution l'indice de valeur precedent
        //offrant la plus grande probabilité
        forward.setMaxPrevious(sum.getMaxPrevious());
    }

    public List<List<Domain.DomainValue>> mostLikelyPath(int t) {

        LinkedList<List<Domain.DomainValue>> mostLikelySequence = new LinkedList<>();

        //récupère la matrice forward du dernier temps;
        Matrix tForwardMax = forwards.get(t);

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

            tForwardMax = forwards.get(t);

            mostLikelySequence.addFirst(tForwardMax.getRowValue(row));

            row = tForwardMax.getPreviousForwardMaxValueRow(row);

            t--;
        }

        return mostLikelySequence;
    }

}
