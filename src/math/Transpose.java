package math;

import domain.data.AbstractDouble;

public class Transpose extends Matrix {

    public Transpose(Matrix matrix) {

        super(matrix.matrix, matrix.doubleFactory);
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
}
