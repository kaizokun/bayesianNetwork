package math;

import domain.Domain;
import domain.data.AbstractDouble;

import java.util.List;

public class Transpose extends Matrix {

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

        if(dependencies != null)
            builder.append("ROWS : "+dependencies+'\n');
        if(variables != null)
            builder.append("COLS : "+variables+'\n');

        if(!this.isObservation && this.parentValues != null){

            builder.append(String.format("%6s", ""));

            for( List<Domain.DomainValue> domainValues : parentValues){

                builder.append(String.format("%-7s", domainValues));
            }
        }

        builder.append('\n');

        for(int r = 0 ; r < this.getRowCount(); r ++){

            if(values != null){

                builder.append(String.format("%5s", values.get(r)));
            }else{

                builder.append(String.format("%5s", ""));
            }

            for(int c = 0 ; c < this.getColCount(); c ++){

                builder.append(String.format("[%.3f]", getValue(r, c).getDoubleValue()));
            }

            builder.append('\n');
        }

        return builder.toString();
    }
}
