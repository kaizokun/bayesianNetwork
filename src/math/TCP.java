package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.Variable;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class TCP extends Matrix {

    protected Map<List<Domain.DomainValue>, Integer> rowIndex = new Hashtable<>(),
            colIndex = new Hashtable<>();

    public TCP(AbstractDouble[][] matrix,
               List<Variable> rowVars, List<List<Domain.DomainValue>> rowValues,
               List<Variable> colVars, List<List<Domain.DomainValue>> colValues,
               AbstractDoubleFactory doubleFactory) {

        super(matrix, rowVars, rowValues, colVars, colValues, doubleFactory);

        this.loadIndex(rowIndex, rowValues);

        this.loadIndex(colIndex, colValues);
    }

    private <T> void loadIndex(Map<T, Integer> index, List<T> objects) {

        int i = 0;

        for (T object : objects) {

            index.put(object, i++);
        }
    }

    public AbstractDouble getProbability(List<Domain.DomainValue> parentValues, List<Domain.DomainValue> values) {

        //System.out.println("getProbability "+this.matrix[rowIndex.get(parentValues)][colIndex.get(values)]);

        return this.matrix[rowIndex.get(parentValues)][colIndex.get(values)];
    }
}
