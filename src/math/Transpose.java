package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.Variable;

import java.util.List;

public class Transpose extends Matrix {



    public Transpose(AbstractDouble[][] matrix,
                     List<Variable> rowVars,
                     List<Domain.DomainValue> rowValues,
                     List<Variable> colVars,
                     List<Domain.DomainValue> colValues,
                     AbstractDoubleFactory doubleFactory) {

        super(matrix, rowVars, rowValues, colVars, colValues, doubleFactory);
    }

    public Transpose(AbstractDouble[][] matrix,
                     List<Variable> colVars,
                     List<Domain.DomainValue> colValues,
                     AbstractDoubleFactory doubleFactory) {

        super(matrix, null, null, colVars, colValues, doubleFactory);
    }

    public Transpose() { }

    public Transpose(Matrix matrix) {

        super(matrix);
    }

    @Override
    public AbstractDouble getValue(int row, int col) {

        return super.getValue(col, row);
    }

    public AbstractDouble getValue(int row) {

        return matrix[0][row];
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
    public Matrix getNew() {

        return new Transpose();
    }

    @Override
    public List<Domain.DomainValue> getColValues() {
        return super.getRowValues();
    }

    @Override
    public List<Domain.DomainValue> getRowValues() {
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
