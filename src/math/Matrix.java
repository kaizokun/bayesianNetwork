package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.List;

public class Matrix {

    protected AbstractDouble[][] matrix;

    protected AbstractDoubleFactory doubleFactory;

    public Matrix() {
    }

    public Matrix(AbstractDouble[][] matrix, AbstractDoubleFactory doubleFactory) {

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

        Matrix rsMatrix = new Matrix(new AbstractDouble[this.getRowCount()][m2.getColCount()], doubleFactory);

        for (int row = 0; row < this.getRowCount(); row++) {

            for (int col = 0; col < m2.getColCount(); col++) {

                AbstractDouble sum = doubleFactory.getNew(0.0);
                //autant de somme de que de colones dans m1 (ou de ligne dans m2)
                for (int cr = 0; cr < this.getColCount(); cr++) {

                    sum = sum.add(this.getValue(row, cr).multiply(m2.getValue(cr, col )));
                }

                rsMatrix.setValue(row, col, sum);
            }
        }

        return rsMatrix;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("\n");

        for(AbstractDouble[] row : matrix){

            for(AbstractDouble col : row){

                builder.append(String.format("[%.3f]", col.getDoubleValue()));
            }

            builder.append('\n');
        }

        return builder.toString();
    }
}
