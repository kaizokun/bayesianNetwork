package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.Variable;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Distribution extends Transpose {

    public Map<Domain.DomainValue, Integer> valuesIndex = new Hashtable<>();

    public AbstractDouble total;

    public Distribution(AbstractDouble[][] matrix, List<Variable> rowVars, List<Domain.DomainValue> rowValues,
                        List<Variable> colVars, List<Domain.DomainValue> colValues,
                        AbstractDoubleFactory doubleFactory, Map<Domain.DomainValue, Integer> valuesIndex) {
        super(matrix, rowVars, rowValues, colVars, colValues, doubleFactory);

        this.valuesIndex = valuesIndex;
    }

    public Distribution(Distribution distribution) {

        super(distribution.newMatrix(),
                distribution.rowVars, distribution.rowValues,
                distribution.colVars, distribution.colValues,
                distribution.doubleFactory);

        for (int row = 0; row < distribution.getRowCount(); row++) {

            this.setValue(row, this.doubleFactory.getNew(distribution.getValue(row).getDoubleValue()));
        }

        this.total = this.doubleFactory.getNew(distribution.getTotal().getDoubleValue());

        this.initValuesIndex();
    }

    public Distribution(Variable megaRequest, AbstractDoubleFactory doubleFactory) {

        super(new AbstractDouble[1][megaRequest.getDomainValues().size()],
                null, null,
                megaRequest.getCompoVars(), megaRequest.getDomainValues(),
                doubleFactory);

        this.doubleFactory = doubleFactory;

        this.initValuesIndex();
    }

    private void initValuesIndex() {

        int row = 0;

        for (Domain.DomainValue domainValue : this.getRowValues()) {

            valuesIndex.put(domainValue, row++);
        }
    }

    public void put(Domain.DomainValue value, AbstractDouble prob) {

        this.setValue(valuesIndex.get(value), prob);
    }

    public AbstractDouble get(Domain.DomainValue value) {

        return this.getValue(valuesIndex.get(value));
    }

    public void putTotal(AbstractDouble total) {

        this.total = total;
    }

    public AbstractDouble getTotal() {

        if (total == null) {

            total = doubleFactory.getNew(0.0);

            for (AbstractDouble prob : matrix[0]) {

                total = total.add(prob);
            }
        }

        return total;
    }

    @Override
    public String toString() {

        return super.toString() + "\nTOTAL : " + getTotal() + "\n";

    }

    public Distribution normalize() {

        for (int row = 0; row < getRowCount(); row++) {

            setValue(row, getValue(row).divide(getTotal()));
        }

        this.total = null;

        this.getTotal();

        return this;
    }

    public Distribution multiply(Distribution d2) {

        System.out.println(this);

        System.out.println(d2);

        AbstractDouble[][] matrix = newMatrix();

        Distribution rs = new Distribution(matrix, null, null, this.colVars,
                this.colValues, this.doubleFactory, this.valuesIndex);

        for (int row = 0; row < this.getRowCount(); row++) {

            rs.setValue(row, this.getValue(row).multiply(d2.getValue(row)));
        }

        return rs;
    }

    private void setValue(int row, AbstractDouble value) {

        this.matrix[0][row] = value;
    }

    private AbstractDouble[][] newMatrix() {
        //nouvelle sous matrice le nombre de colonnes correspond au nombre de ligne de la distribution
        //étant une transposée verticale d'une matrice horyzontale
        return new AbstractDouble[1][this.getRowCount()];
    }

}
