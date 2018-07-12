package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.Variable;
import java.util.Hashtable;
import java.util.Map;

public class Distribution extends Matrix {

    public Map<Domain.DomainValue, Integer> valuesIndex = new Hashtable<>();

    public AbstractDouble total;

    public AbstractDoubleFactory doubleFactory;

    public Distribution(Distribution distribution) {

        this.matrix = new AbstractDouble[1][distribution.getColCount()];

        for (int col = 0; col < distribution.getColCount(); col++) {

            matrix[0][col] = doubleFactory.getNew(distribution.getValue(0, col).getDoubleValue());
        }

        this.colValues = distribution.getColValues();

        this.colVars = distribution.getColVars();

        this.total = doubleFactory.getNew(distribution.getTotal().getDoubleValue());

        this.doubleFactory = distribution.doubleFactory;

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

    public Distribution() {

    }

    private void initValuesIndex(){

        int c = 0;

        for (Domain.DomainValue domainValue : this.getColValues()) {

            valuesIndex.put(domainValue, c++);
        }
    }

    public void put(Domain.DomainValue value, AbstractDouble prob) {

        this.setValue(0, valuesIndex.get(value), prob);
    }

    public AbstractDouble get(Domain.DomainValue value) {

        return this.getValue(0, valuesIndex.get(value));
    }

    public void putTotal(AbstractDouble total) {

        this.total = total;
    }

    public AbstractDouble getTotal() {

        if(total == null){

            total = doubleFactory.getNew(0.0);

            for(AbstractDouble prob : matrix[0]){

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

        for (int col = 0; col < getRowCount(); col++) {

            setValue(0, col, getValue(0, col).divide(getTotal()));
        }

        return this;
    }

    public Distribution multiply(Distribution d2){

        Distribution rs = new Distribution();

        rs.matrix = new AbstractDouble[1][this.getColCount()];

        rs.colValues = this.getColValues();

        rs.colVars = this.getColVars();

        rs.doubleFactory = this.doubleFactory;

        rs.initValuesIndex();

        for(int col = 0 ; col < this.getColCount() ; col ++){

            rs.setValue(0, col, this.getValue(0, col).multiply(d2.getValue(0, col)));
        }

        return rs;
    }

}
