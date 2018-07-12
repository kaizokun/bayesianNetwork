package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.Variable;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Distribution extends Matrix {

    public Map<Domain.DomainValue, Integer> valuesIndex = new Hashtable<>();

    public AbstractDouble total;

    public Distribution(AbstractDouble[][] matrix,
                        List<Variable> rowVars,
                        List<Domain.DomainValue> rowValues,
                        List<Variable> colVars,
                        List<Domain.DomainValue> colValues,
                        AbstractDoubleFactory doubleFactory) {

        super(matrix, rowVars, rowValues, colVars, colValues, doubleFactory);
    }

    public Distribution(Variable megaRequest, AbstractDoubleFactory doubleFactory) {

        super(new AbstractDouble[1][megaRequest.getDomainValues().size()],
                null, null,
                megaRequest.getCompoVars(), megaRequest.getDomainValues(),
                doubleFactory);

        int c = 0;

        for (Domain.DomainValue domainValue : this.getColValues()) {

            valuesIndex.put(domainValue, c++);
        }
    }

    public void put(Domain.DomainValue value, AbstractDouble prob) {

        this.setValue(0, valuesIndex.get(value), prob);
    }

    public AbstractDouble get(Domain.DomainValue value){

       return this.getValue(0, valuesIndex.get(value));
    }

    public void putTotal(AbstractDouble total){

        this.total = total;
    }

    public AbstractDouble getTotal() {
        return total;
    }

    @Override
    public String toString() {

        return super.toString()+"\nTOTAL : "+total+"\n";

    }
}
