package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.Variable;

import java.util.*;

public class Matrix {

    protected AbstractDouble[][] matrix;

    protected AbstractDoubleFactory doubleFactory;
    //pour l'affichage des matrices
    protected List<Domain.DomainValue> colValues, rowValues;

    protected List<Variable> colVars, rowVars;

    protected Map<Integer, Integer> maxPrevious = new Hashtable<>();

    protected boolean isObservation = false;

    public Matrix() { }

    public Matrix(Matrix matrix) {

        this(matrix.matrix, matrix.rowVars, matrix.rowValues, matrix.colVars, matrix.colValues, matrix.doubleFactory);
    }

    public Matrix(AbstractDouble[][] matrix, AbstractDoubleFactory doubleFactory) {

        this(matrix, null, null, null, null, doubleFactory);
    }

    public Matrix(AbstractDouble[][] matrix,
                  List<Variable> rowVars,
                  List<Domain.DomainValue> rowValues,
                  List<Variable> colVars,
                  List<Domain.DomainValue> colValues,
                  AbstractDoubleFactory doubleFactory) {

        this(matrix, rowVars, rowValues, colVars, colValues, doubleFactory, false);
    }

    public Matrix(AbstractDouble[][] matrix,
                  List<Variable> rowVars,
                  List<Domain.DomainValue> rowValues,
                  List<Variable> colVars,
                  List<Domain.DomainValue> colValues,
                  AbstractDoubleFactory doubleFactory,
                  boolean isObservation) {

        this.matrix = matrix;

        this.setRowVars(rowVars);

        this.setRowValues(rowValues);

        this.setColVars(colVars);

        this.setColValues(colValues);

        this.doubleFactory = doubleFactory;

        this.isObservation = isObservation;
    }

    public Matrix copy() {

        //important une matrice inverse doit rester du même type
        //par exemple la matrice observation une fois inversé doit rester une matrice diagonale
        //necessaire pour la multiplication ligne par ligne on on joue sur le polymorphisme
        //pour traiter une matrice diagonale comme une matrice colonne
        //elle doit cependant avoir une forme carré pour etre inversée
        Matrix copy = this.getNew();

        copy.matrix = new AbstractDouble[this.getRowCount()][this.getColCount()];

        for (int row = 0; row < this.getRowCount(); row++) {

            for (int col = 0; col < this.getColCount(); col++) {

                copy.setValue(row, col, getValue(row, col).copy());
            }
        }

        copy.doubleFactory = this.doubleFactory;

        copy.colValues = colValues;

        copy.rowValues = rowValues;

        copy.colVars = colVars;

        copy.rowVars = rowVars;

        copy.isObservation = isObservation;

        return copy;
    }

    public Matrix getNew() {

        return new Matrix();
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

    public AbstractDouble getValue(int row) {

        return matrix[row][0];
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

    public Matrix multiplyRows(Matrix m2) {

        Matrix rs = new Transpose(new AbstractDouble[1][this.getRowCount()], m2.getRowVars(), m2.getRowValues(), doubleFactory);

        for (int row = 0; row < getRowCount(); row++) {

            rs.setValue(row, 0, this.getValue(row).multiply(m2.getValue(row)));
        }

        return rs;
    }

    public void setMaxPreviousRow(int forwardRow, int maxPreviousForwardRow) {

        this.maxPrevious.put(forwardRow, maxPreviousForwardRow);
    }

    public Matrix normalize() {

        AbstractDouble total = doubleFactory.getNew(0.0);

        for (int row = 0; row < getRowCount(); row++) {

            total = total.add(getValue(row, 0));
        }

        for (int row = 0; row < getRowCount(); row++) {

            setValue(row, 0, getValue(row, 0).divide(total));
        }

        return this;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("\n");

        toString(builder, "");

        return builder.toString();
    }

    public String toString(String ident) {

        StringBuilder builder = new StringBuilder("\n");

        toString(builder, ident);

        return builder.toString();
    }

    private void toString(StringBuilder builder, String ident) {

        if (this.getRowVars() != null) {

            builder.append(ident + "ROWS : " + this.getRowVars() + '\n');
        }

        if (this.getColVars() != null) {

            builder.append(ident + "COLS : " + this.getColVars() + '\n');
        }

        if (!this.isObservation && this.getColValues() != null) {

            builder.append(ident + String.format("%6s", ""));

            for (Domain.DomainValue domainValues : this.getColValues()) {

                builder.append(String.format("%-7s", domainValues));
            }
        }

        builder.append('\n');

        for (int r = 0; r < this.getRowCount(); r++) {

            if (this.getRowValues() != null) {

                builder.append(ident + String.format("%5s", this.getRowValues().get(r)));

            } else {

                builder.append(ident + String.format("%5s", ""));
            }

            for (int c = 0; c < this.getColCount(); c++) {

                builder.append(String.format("[%.3f]", getValue(r, c).getDoubleValue()));
            }

            builder.append('\n');
        }

    }


    public List<Domain.DomainValue> getColValues() {
        return colValues;
    }

    public List<Domain.DomainValue> getRowValues() {
        return rowValues;
    }

    public List<Variable> getColVars() {
        return colVars;
    }

    public List<Variable> getRowVars() {
        return rowVars;
    }

    public void setColValues(List<Domain.DomainValue> colValues) {
        this.colValues = colValues;
    }

    public void setRowValues(List<Domain.DomainValue> rowValues) {
        this.rowValues = rowValues;
    }

    public void setColVars(List<Variable> colVars) {
        this.colVars = colVars;
    }

    public void setRowVars(List<Variable> rowVars) {
        this.rowVars = rowVars;
    }

    public AbstractDoubleFactory getDoubleFactory() {
        return doubleFactory;
    }

    public void setDoubleFactory(AbstractDoubleFactory doubleFactory) {
        this.doubleFactory = doubleFactory;
    }

    public int getPreviousForwardMaxValueRow(Domain.DomainValue... values) {

        //récupere la ligne correspondant à la valeur voulu
        int row = this.getRowValues().indexOf(Arrays.asList(values));
        //retourne l'indice de la ligne pour la valeur max precedente
        return maxPrevious.get(row);
    }

    public int getPreviousForwardMaxValueRow(int row) {
        //retourne l'indice de la ligne pour la valeur max precedente
        return maxPrevious.get(row);
    }

    public Domain.DomainValue getRowValue(int row) {
        //retourne la valeur de ligne
        return this.getRowValues().get(row);
    }

    public Map<Integer, Integer> getMaxPrevious() {
        return maxPrevious;
    }

    public void setMaxPrevious(Map<Integer, Integer> maxPrevious) {
        this.maxPrevious = maxPrevious;
    }



    /*------------------------*/


}
