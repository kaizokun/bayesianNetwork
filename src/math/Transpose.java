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
    public String toString() {

        StringBuilder builder = new StringBuilder("\n");

        if (rowVars != null)
            builder.append("ROWS : " + rowVars + '\n');
        if (colVars != null)
            builder.append("COLS : " + colVars + '\n');

        if (!this.isObservation && this.rowValues != null) {

            builder.append(String.format("%6s", ""));

            for (List<Domain.DomainValue> domainValues : rowValues) {

                builder.append(String.format("%-7s", domainValues));
            }
        }

        builder.append('\n');

        for (int r = 0; r < this.getRowCount(); r++) {

            if (colValues != null) {

                builder.append(String.format("%5s", colValues.get(r)));
            } else {

                builder.append(String.format("%5s", ""));
            }

            for (int c = 0; c < this.getColCount(); c++) {

                builder.append(String.format("[%.3f]", getValue(r, c).getDoubleValue()));
            }

            builder.append('\n');
        }

        return builder.toString();
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
