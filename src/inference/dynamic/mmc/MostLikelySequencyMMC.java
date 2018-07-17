package inference.dynamic.mmc;

import domain.Domain;
import domain.data.AbstractDouble;
import math.Matrix;
import math.MatrixUtil;
import network.dynamic.MMC;

import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MostLikelySequencyMMC extends ForwardMMC {

    public MostLikelySequencyMMC(MMC mmc) {

        super(mmc);
    }

    @Override
    protected Map.Entry<Integer, Matrix> getLastDistribution() {

        return this.mmc.getLastMax();
    }

    public Matrix mostLikelySequence() {

        return this.forward(true);
    }

    @Override
    protected Matrix multiplyTransitionForward(Matrix matrixStatesT, Matrix forward) {
        //multiplication qui differe de la multiplication habituelle
        //ou plutot que de sommer sur chaque valeur état parent on sauvegarde le maximum
        return MatrixUtil.multiplyMax(matrixStatesT, forward);
    }

    @Override
    protected void mostLikelyPath(Matrix forward, Matrix sum) {
        //enregsitre pour chaque indice de valeur de la distribution l'indice de valeur precedent
        //offrant la plus grande probabilité
        forward.setMaxPrevious(sum.getMaxPrevious());
    }

    public Matrix mostLikelySequency(int t) {

        return forward(t, true);
    }

    public List mostLikelyPath(int t) {

        LinkedList<Map.Entry<Integer, Domain.DomainValue>> mostLikelySequence = new LinkedList<>();

        //récupère la matrice forward du dernier temps;
        Matrix tForwardMax = forwards.get(t);

        AbstractDouble max = mmc.getDoubleFactory().getNew(0.0);

        int maxRow = 0;
        //max probabilité
        for (int row = 0; row < tForwardMax.getRowCount(); row++) {

            if (tForwardMax.getValue(row, 0).compareTo(max) > 0) {

                max = tForwardMax.getValue(row, 0);

                maxRow = row;
            }
        }

        mostLikelySequence.add(new AbstractMap.SimpleEntry<>(t, tForwardMax.getRowValue(maxRow)));

        //récupere la ligne correspondant à la meilleur valeur precedente
        //à partir des valeurs recu en parametre
        int row = tForwardMax.getPreviousForwardMaxValueRow(maxRow);

        t--;

        while (t > 0) {

            tForwardMax = forwards.get(t);

            mostLikelySequence.addFirst(new AbstractMap.SimpleEntry<>(t, tForwardMax.getRowValue(row)));

            row = tForwardMax.getPreviousForwardMaxValueRow(row);

            t--;
        }

        return mostLikelySequence;
    }


}
