package math;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.Variable;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class Matrix {

    protected AbstractDouble[][] matrix;

    protected AbstractDoubleFactory doubleFactory;
    //pour l'affichage des matrices
    protected List<List<Domain.DomainValue>> colValues, rowValues;

    protected List<Variable> colVars, rowVars;

    protected Map<Integer, Integer> maxPrevious = new Hashtable<>();

    protected boolean isObservation = false;

    public Matrix() {
    }

    public Matrix(Matrix matrix) {

        this(matrix.matrix, matrix.rowVars, matrix.rowValues, matrix.colVars, matrix.colValues, matrix.doubleFactory);
    }

    public Matrix(AbstractDouble[][] matrix, AbstractDoubleFactory doubleFactory) {

        this(matrix, null, null, null, null, doubleFactory);
    }


    public Matrix(AbstractDouble[][] matrix,
                  List<Variable> rowVars,
                  List<List<Domain.DomainValue>> rowValues,
                  List<Variable> colVars,
                  List<List<Domain.DomainValue>> colValues,
                  AbstractDoubleFactory doubleFactory) {

        this(matrix, rowVars, rowValues, colVars, colValues, doubleFactory, false);

    }

    public Matrix(AbstractDouble[][] matrix,
                  List<Variable> rowVars,
                  List<List<Domain.DomainValue>> rowValues,
                  List<Variable> colVars,
                  List<List<Domain.DomainValue>> colValues,
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

    public static Matrix multiplyMax(Matrix m1, Matrix m2) {

        if (m1.getColCount() != m2.getRowCount()) {

            throw new RuntimeException("le nombre de colones de la matrice ne correspond pas avec" +
                    " le nombre de lignes de celle reçu en parametres");
        }

        Matrix rsMaxMatrix;

        if (m2.getColCount() == 1) {

            rsMaxMatrix = new Transpose(
                    new AbstractDouble[1][m2.getRowCount()],
                    null, null,
                    m2.getRowVars(), m2.getRowValues(),
                    m1.getDoubleFactory());

        } else {

            rsMaxMatrix = new Matrix(
                    new AbstractDouble[m1.getRowCount()][m2.getColCount()],
                    null, null, null, null,
                    m1.getDoubleFactory());
        }

        //pour chaque ligne on travaille sur une valeur de la variable état enfant
        for (int row = 0; row < m1.getRowCount(); row++) {
            //le nombre de colone de sum si il s'agit du sum est unique
            for (int col = 0; col < m2.getColCount(); col++) {

                AbstractDouble max = m1.doubleFactory.getNew(0.0);

                int maxPreviousForwardRow = 0;
                //autant de terme dans la somme de que de colones dans m1 (ou de ligne dans sum)
                //chaque partie de la somme correspond à une multiplication entre
                //la valeur courante de l'état enfant (une par ligne) conditionné par une valeur de l'état parent
                //et la valeur sum pour une valeur de l'état parent
                //identique pour le max sauf qu'on prend le maximum plutot qu'additionner
                for (int cr = 0; cr < m1.getColCount(); cr++) {

                    AbstractDouble mulMax = m1.getValue(row, cr).multiply(m2.getValue(cr, col));

                    if (mulMax.compareTo(max) > 0) {

                        max = mulMax;
                        //enregistre l'indice de la ligne du maxForward qui à fournit la valeur max.
                        maxPreviousForwardRow = cr;
                    }
                }

                rsMaxMatrix.setValue(row, col, max);
                //pour tel ligne de la matrice max resultat ( ou colone de la transposée de la matrice de transition )
                //indique quelle valeur de l'état precedent offre la probabilité maximum,
                rsMaxMatrix.setMaxPreviousRow(row, maxPreviousForwardRow);
            }
        }

        return rsMaxMatrix;
    }

    public static Matrix multiply(Matrix m1, Matrix m2) {

        if (m1.getColCount() != m2.getRowCount()) {

            throw new RuntimeException("le nombre de colones de la matrice ne correspond pas avec" +
                    " le nombre de lignes de celle reçu en parametres");
        }

        Matrix rsMatrix;

        if (m2.getColCount() == 1) {

            rsMatrix = new Transpose(
                    new AbstractDouble[1][m2.getRowCount()],
                    null, null,
                    m2.getRowVars(), m2.getRowValues(),
                    m1.getDoubleFactory());

        } else {

            rsMatrix = new Matrix(
                    new AbstractDouble[m1.getRowCount()][m2.getColCount()],
                    null, null, null, null,
                    m1.getDoubleFactory());
        }

        //pour chaque ligne on travaille sur une valeur de la variable état enfant
        for (int row = 0; row < m1.getRowCount(); row++) {
            //le nombre de colone de m2 si il s'agit du sum est unique
            for (int col = 0; col < m2.getColCount(); col++) {

                AbstractDouble sum = m1.getDoubleFactory().getNew(0.0);

                //autant de terme dans la somme de que de colones dans m1 (ou de ligne dans m2)
                //chaque partie de la somme correspond à une multiplication entre
                //la valeur courante de l'état enfant (une par ligne) conditionné par une valeur de l'état parent
                //et la valeur sum pour une valeur de l'état parent
                for (int cr = 0; cr < m1.getColCount(); cr++) {

                    AbstractDouble mul = m1.getValue(row, cr).multiply(m2.getValue(cr, col));

                    sum = sum.add(mul);
                }

                rsMatrix.setValue(row, col, sum);
            }
        }

        return rsMatrix;
    }

    public Matrix multiplyRows(Matrix m2) {

        Matrix rs = new Transpose(new AbstractDouble[1][this.getRowCount()], m2.getRowVars(), m2.getRowValues(), doubleFactory);

        for (int row = 0; row < getRowCount(); row++) {

            rs.setValue(row, 0, this.getValue(row).multiply(m2.getValue(row)));
        }

        return rs;
    }

    private void setMaxPreviousRow(int forwardRow, int maxPreviousForwardRow) {

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

            for (List<Domain.DomainValue> domainValues : this.getColValues()) {

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


    public List<List<Domain.DomainValue>> getColValues() {
        return colValues;
    }

    public List<List<Domain.DomainValue>> getRowValues() {
        return rowValues;
    }

    public List<Variable> getColVars() {
        return colVars;
    }

    public List<Variable> getRowVars() {
        return rowVars;
    }

    public void setColValues(List<List<Domain.DomainValue>> colValues) {
        this.colValues = colValues;
    }

    public void setRowValues(List<List<Domain.DomainValue>> rowValues) {
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

    public List<Domain.DomainValue> getRowValue(int row) {
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

    public static Matrix invert(Matrix matrix) {

        Matrix matrixCopy = matrix.copy();

        AbstractDouble[][] invertMatrix = invert(matrixCopy, matrixCopy.doubleFactory);

        matrixCopy.setMatrix(invertMatrix);

        return matrixCopy;
    }

    public static AbstractDouble[][] invert(Matrix matrix, AbstractDoubleFactory doubleFactory) {

        int n = matrix.getRowCount();

        AbstractDouble x[][] = new AbstractDouble[n][n];

        AbstractDouble b[][] = new AbstractDouble[n][n];

        initMatrixZero(x, doubleFactory);

        initMatrixZero(b, doubleFactory);

        int index[] = new int[n];

        for (int i = 0; i < n; ++i) {

            b[i][i] = doubleFactory.getNew(1.0);
        }

        // Transform the matrix into an upper triangle
        gaussian(matrix, index, doubleFactory);

        // Update the matrix b[i][j] with the ratios stored

        for (int i = 0; i < n - 1; ++i) {

            for (int j = i + 1; j < n; ++j) {

                for (int k = 0; k < n; ++k) {

                    b[index[j]][k] = b[index[j]][k].substract(matrix.getValue(index[j], i).multiply(b[index[i]][k]));
                }
            }
        }

        // Perform backward substitutions

        for (int i = 0; i < n; ++i) {

            x[n - 1][i] = b[index[n - 1]][i].divide(matrix.getValue(index[n - 1], n - 1));

            for (int j = n - 2; j >= 0; --j) {

                x[j][i] = b[index[j]][i];

                for (int k = j + 1; k < n; ++k) {

                    x[j][i] = x[j][i].substract(matrix.getValue(index[j], k).multiply(x[k][i]));

                }

                x[j][i] = x[j][i].divide(matrix.getValue(index[j], j));
            }
        }

        return x;
    }

    public static void initMatrixZero(AbstractDouble[][] x, AbstractDoubleFactory doubleFactory) {

        int rows = x.length, cols = x[0].length;

        for (int row = 0; row < rows; row++) {

            for (int col = 0; col < cols; col++) {

                x[row][col] = doubleFactory.getNew(0.0);
            }
        }
    }

    public static void gaussian(Matrix matrix, int index[], AbstractDoubleFactory doubleFactory) {

        int n = index.length;

        AbstractDouble c[] = new AbstractDouble[n];

        // Initialize the index
        for (int i = 0; i < n; ++i) {

            index[i] = i;
        }

        // Find the rescaling factors, one from each row

        for (int i = 0; i < n; ++i) {

            AbstractDouble c1 = doubleFactory.getNew(0.0);

            for (int j = 0; j < n; ++j) {

                AbstractDouble c0 = matrix.getValue(i, j).abs();

                if (c0.compareTo(c1) > 0) {

                    c1 = c0;
                }
            }
            c[i] = c1;
        }

        // Search the pivoting element from each column
        int k = 0;

        for (int j = 0; j < n - 1; ++j) {

            AbstractDouble pi1 = doubleFactory.getNew(0.0);

            for (int i = j; i < n; ++i) {

                AbstractDouble pi0 = matrix.getValue(index[i], j).abs();

                pi0 = pi0.divide(c[index[i]]);

                if (pi0.compareTo(pi1) > 0) {

                    pi1 = pi0;

                    k = i;
                }
            }

            // Interchange rows according to the pivoting order

            int itmp = index[j];

            index[j] = index[k];

            index[k] = itmp;

            for (int i = j + 1; i < n; ++i) {

                AbstractDouble pj = matrix.getValue(index[i], j).divide(matrix.getValue(index[j], j));
                //AbstractDouble pj = a[index[i]][j].divide(a[index[j]][j]);

                //Record pivoting ratios below the diagonal

                matrix.setValue(index[i], j, pj);

                //Modify other elements accordingly

                for (int l = j + 1; l < n; ++l) {

                    //a[index[i]][l] -= pj * a[index[j]][l];
                    matrix.setValue(index[i], l, matrix.getValue(index[i], l).substract(pj.multiply(matrix.getValue(index[j], l))));
                }
            }
        }
    }

}
