package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.Variable;

import java.util.List;

public class MatrixDiagonal extends Matrix {

    protected Matrix colMatrix;

    public MatrixDiagonal() { }

    public MatrixDiagonal(AbstractDouble[][] matrix, List<Variable> rowVars, List<List<Domain.DomainValue>> rowValues
            , List<Variable> colVars, List<List<Domain.DomainValue>> colValues,
                          AbstractDoubleFactory doubleFactory, boolean isObservation) {

        super(matrix, rowVars, rowValues, colVars, colValues, doubleFactory, isObservation);

        this.initColmatrix();
    }

    public AbstractDouble getValue(int row) {

        return matrix[row][row];
    }

    public void initColmatrix() {

        //crée une transposee à partir d'une matrice ayant 1 ligne et autant de colonne
        //que de ligne (ou de colonnes) dans la matrice carré  diagonale
        //la transposée inverse les colones et les lignes
        this.colMatrix = new Transpose(new AbstractDouble[1][this.getRowCount()], this.getRowVars(), this.getRowValues(), doubleFactory);

        for (int row = 0; row < this.colMatrix.getRowCount(); row++) {

            this.colMatrix.setValue(row, 0, this.getValue(row));
        }

    }

    public Matrix getColMatrix() {

        return colMatrix;
    }

    @Override
    public Matrix getNew() {

        return new MatrixDiagonal();
    }
}
