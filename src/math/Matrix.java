package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.Variable;

import java.util.List;

public class Matrix {

    protected AbstractDouble[][] matrix;

    protected AbstractDoubleFactory doubleFactory;
    //pour l'affichage des matrices
    protected List<List<Domain.DomainValue>> values, parentValues;

    protected List<Variable> variables, dependencies;

    protected boolean isObservation = false;

    public Matrix() { }

    public Matrix(Matrix matrix) {

        this(matrix.matrix, matrix.variables, matrix.dependencies, matrix.doubleFactory, matrix.isObservation);
    }

    public Matrix(AbstractDouble[][] matrix, AbstractDoubleFactory doubleFactory) {

        this(matrix, null, null, doubleFactory, false);
    }

    public Matrix(AbstractDouble[][] matrix, List<Variable> variables, List<Variable> dependencies,
                  AbstractDoubleFactory doubleFactory, boolean isObservation) {

        //pour l'affichage des matrices

        this.variables = variables;

        this.dependencies = dependencies;

        if (variables != null) {

            this.values = BayesianNetwork.domainValuesCombinations(variables);
        }

        if (dependencies != null) {

            this.parentValues = BayesianNetwork.domainValuesCombinations(dependencies);
        }

        this.isObservation = isObservation;

        //----
        this.matrix = matrix;

        this.doubleFactory = doubleFactory;
    }

    public int getRowCount() {

        return this.matrix.length;
    }

    public int getColCount() {

        return this.matrix[0].length;
    }

    public AbstractDouble getValue(int row, int col) {

        return matrix[row][col];
    }

    public void setValue(int row, int col, AbstractDouble value) {

        this.matrix[row][col] = value;
    }

    public AbstractDouble[][] getMatrix() {

        return matrix;
    }

    public void setMatrix(AbstractDouble[][] matrix) {

        this.matrix = matrix;
    }

    public Matrix multiply(Matrix m2) {

        if (this.getColCount() != m2.getRowCount()) {

            throw new RuntimeException("le nombre de colones de la matrice ne correspond pas avec" +
                    " le nombre de lignes de celle reçu en parametres");
        }
        //la matrice resultat à autemp de ligne que m1 et autant de colones que m2

        Matrix rsMatrix = new Matrix(new AbstractDouble[this.getRowCount()][m2.getColCount()], null, null, doubleFactory, false);

        for (int row = 0; row < this.getRowCount(); row++) {

            for (int col = 0; col < m2.getColCount(); col++) {

                AbstractDouble sum = doubleFactory.getNew(0.0);
                //autant de somme de que de colones dans m1 (ou de ligne dans m2)
                for (int cr = 0; cr < this.getColCount(); cr++) {

                    sum = sum.add(this.getValue(row, cr).multiply(m2.getValue(cr, col)));
                }

                rsMatrix.setValue(row, col, sum);
            }
        }

        return rsMatrix;
    }

    public Matrix normalize(){

        AbstractDouble total = doubleFactory.getNew(0.0);

        for( int row = 0 ; row < getRowCount() ; row ++){

            total = total.add(getValue(row, 0));
        }

        for( int row = 0 ; row < getRowCount() ; row ++){

            setValue(row, 0,getValue(row, 0).divide(total));
        }

        return this;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("\n");

        if (variables != null)
            builder.append("ROWS : " + variables + '\n');
        if (dependencies != null)
            builder.append("COLS : " + dependencies + '\n');

        if (!this.isObservation && this.values != null) {

            builder.append(String.format("%6s", ""));

            for (List<Domain.DomainValue> domainValues : values) {

                builder.append(String.format("%-7s", domainValues));
            }
        }

        builder.append('\n');

        for (int r = 0; r < this.getRowCount(); r++) {

            if (parentValues != null) {

                builder.append(String.format("%5s", parentValues.get(r)));
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

    public Matrix multiplyRows(Matrix m2) {

        Matrix rs = new Matrix(new AbstractDouble[this.getRowCount()][this.getColCount()], doubleFactory);

        for( int row = 0 ; row < getRowCount() ; row ++){

            rs.setValue(row, 0, this.getValue(row, 0).multiply(m2.getValue(row,0)));
        }

        return rs;
    }
}
