package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.Variable;

import java.util.List;

public class Transpose extends Matrix {

    public Transpose(AbstractDouble[][] matrix, List<Variable> colVars, List<Variable> rowVars,
                     AbstractDoubleFactory doubleFactory, boolean isObservation) {
        super(matrix, colVars, rowVars, doubleFactory, isObservation);
    }

    public Transpose(Matrix matrix) {

        super(matrix);
    }

    @Override
    public AbstractDouble getValue(int row, int col) {

        return super.getValue(col, row);
    }

    @Override
    public void setValue(int row, int col, AbstractDouble value) {

        super.setValue(col, row, value);
    }

    @Override
    public int getRowCount() {

        return super.getColCount();
    }

    @Override
    public int getColCount() {

        return super.getRowCount();
    }

    @Override
    public List<List<Domain.DomainValue>> getColValues() {
        return super.getRowValues();
    }

    @Override
    public List<List<Domain.DomainValue>> getRowValues() {
        return super.getColValues();
    }

    @Override
    public List<Variable> getColVars() {
        return super.getRowVars();
    }

    @Override
    public List<Variable> getRowVars() {
        return super.getColVars();
    }
}
