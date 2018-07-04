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

    public Matrix() { }

    public Matrix(Matrix matrix) {

        this(matrix.matrix, matrix.colVars, matrix.rowVars, matrix.doubleFactory, matrix.isObservation);
    }

    public Matrix(AbstractDouble[][] matrix, AbstractDoubleFactory doubleFactory) {

        this(matrix, null, null, doubleFactory, false);
    }

    public Matrix(AbstractDouble[][] matrix, List<Variable> colVars, List<Variable> rowVars,
                  AbstractDoubleFactory doubleFactory, boolean isObservation) {

        //pour l'affichage des matrices

        this.colVars = colVars;

        this.rowVars = rowVars;

        if (colVars != null) {

            this.colValues = BayesianNetwork.domainValuesCombinations(colVars);
        }

        if (rowVars != null) {

            this.rowValues = BayesianNetwork.domainValuesCombinations(rowVars);
        }

        this.isObservation = isObservation;

        //----
        this.matrix = matrix;

        this.doubleFactory = doubleFactory;
    }

    /**
     * init a new sum or backward matrix
     */
    public Matrix(AbstractDouble[][] matrix, List<Variable> rowVars, List<List<Domain.DomainValue>> rowValues,
                  AbstractDoubleFactory doubleFactory) {

        this.matrix = matrix;

        this.rowVars = rowVars;

        this.rowValues = rowValues;

        this.doubleFactory = doubleFactory;
    }


    public Matrix copy() {

        Matrix copy = new Matrix();

        copy.matrix = new AbstractDouble[this.getRowCount()][this.getColCount()];

        for(int row = 0 ; row < this.getRowCount() ; row ++){

            for(int col = 0 ; col < this.getColCount() ; col ++){

                copy.matrix[row][col] = matrix[row][col].copy();
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

    public Matrix multiplyMax(Matrix maxForward) {

        if (this.getColCount() != maxForward.getRowCount()) {

            throw new RuntimeException("le nombre de colones de la matrice ne correspond pas avec" +
                    " le nombre de lignes de celle reçu en parametres");
        }

        Matrix rsMaxMatrix = new Matrix(
                new AbstractDouble[this.getRowCount()][maxForward.getColCount()],
                maxForward.getRowVars(),
                maxForward.getRowValues(),
                doubleFactory);

        //pour chaque ligne on travaille sur une valeur de la variable état enfant
        for (int row = 0; row < this.getRowCount(); row++) {
            //le nombre de colone de sum si il s'agit du sum est unique
            for (int col = 0; col < maxForward.getColCount(); col++) {

                AbstractDouble max = doubleFactory.getNew(0.0);

                int maxPreviousForwardRow = 0;
                //autant de terme dans la somme de que de colones dans m1 (ou de ligne dans sum)
                //chaque partie de la somme correspond à une multiplication entre
                //la valeur courante de l'état enfant (une par ligne) conditionné par une valeur de l'état parent
                //et la valeur sum pour une valeur de l'état parent
                //identique pour le max sauf qu'on prend le maximum plutot qu'additionner
                for (int cr = 0; cr < this.getColCount(); cr++) {

                    AbstractDouble mulMax = this.getValue(row, cr).multiply(maxForward.getValue(cr, col));

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

    public Matrix multiply(Matrix m2) {

        if (this.getColCount() != m2.getRowCount()) {

            throw new RuntimeException("le nombre de colones de la matrice ne correspond pas avec" +
                    " le nombre de lignes de celle reçu en parametres");
        }

        //la matrice resultat à autant de ligne que m1 et autant de colones que m2
        //ici on s'en sert principalement pour calculer une distribution
        //sur une variable ou une megavariable, le sum ou le backward ( 1 colonne plusieurs lignes )
        //on passe aussi les colVars et les valeurs qui constitue chaque ligne de la distribution
        Matrix rsMatrix = new Matrix(
                new AbstractDouble[this.getRowCount()][m2.getColCount()],
                m2.getRowVars(),
                m2.getRowValues(),
                doubleFactory);

        //pour chaque ligne on travaille sur une valeur de la variable état enfant
        for (int row = 0; row < this.getRowCount(); row++) {
            //le nombre de colone de m2 si il s'agit du sum est unique
            for (int col = 0; col < m2.getColCount(); col++) {

                AbstractDouble sum = doubleFactory.getNew(0.0);

                //autant de terme dans la somme de que de colones dans m1 (ou de ligne dans m2)
                //chaque partie de la somme correspond à une multiplication entre
                //la valeur courante de l'état enfant (une par ligne) conditionné par une valeur de l'état parent
                //et la valeur sum pour une valeur de l'état parent
                for (int cr = 0; cr < this.getColCount(); cr++) {

                    AbstractDouble mul = this.getValue(row, cr).multiply(m2.getValue(cr, col));

                    sum = sum.add(mul);
                }

                rsMatrix.setValue(row, col, sum);
            }
        }

        return rsMatrix;
    }

    public Matrix multiplyRows(Matrix m2) {

        Matrix rs = new Matrix(new AbstractDouble[this.getRowCount()][this.getColCount()], this.getRowVars(), this.getRowValues(), doubleFactory);

        for (int row = 0; row < getRowCount(); row++) {

            rs.setValue(row, 0, this.getValue(row, 0).multiply(m2.getValue(row, 0)));
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


    // Method to carry out the partial-pivoting Gaussian

    // elimination.  Here index[] stores pivoting order.

    /*
    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("\n");

        if (colVars != null)
            builder.append("COLS : " + colVars + '\n');
        if (rowVars != null)
            builder.append("ROWS : " + rowVars + '\n');

        if (!this.isObservation && this.colValues != null) {

            builder.append(String.format("%6s", ""));

            for (List<Domain.DomainValue> domainValues : colValues) {

                builder.append(String.format("%-7s", domainValues));
            }
        }

        builder.append('\n');

        for (int r = 0; r < this.getRowCount(); r++) {

            if (rowValues != null) {

                builder.append(String.format("%5s", rowValues.get(r)));
            } else {

                builder.append(String.format("%5s", ""));
            }

            for (int c = 0; c < this.getColCount(); c++) {

                builder.append(String.format("[%.3f]", getValue(r, c).getDoubleValue()));
            }

            builder.append('\n');
        }

        builder.append('\n');

        return builder.toString();
    }

    */



    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("\n");

        if (this.getRowVars() != null)
            builder.append("ROWS : " + this.getRowVars() + '\n');
        if (this.getColVars() != null)
            builder.append("COLS : " + this.getColVars() + '\n');

        if (!this.isObservation && this.getColValues() != null) {

            builder.append(String.format("%6s", ""));

            for (List<Domain.DomainValue> domainValues : this.getColValues()) {

                builder.append(String.format("%-7s", domainValues));
            }
        }

        builder.append('\n');

        for (int r = 0; r < this.getRowCount(); r++) {

            if (this.getRowValues() != null) {

                builder.append(String.format("%5s", this.getRowValues().get(r)));

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

    public int getPreviousForwardMaxValueRow(Domain.DomainValue... values) {

        //récupere la ligne correspondant à la valeur voulu
        int row = this.rowValues.indexOf(Arrays.asList(values));
        //retourne l'indice de la ligne pour la valeur max precedente
        return maxPrevious.get(row);
    }

    public int getPreviousForwardMaxValueRow(int row) {
        //retourne l'indice de la ligne pour la valeur max precedente
        return maxPrevious.get(row);
    }

    public List<Domain.DomainValue> getRowValue(int row) {
        //retourne la valeur de ligne
        return this.rowValues.get(row);
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

        AbstractDouble[][] invertMatrix = invert(matrixCopy.matrix, matrixCopy.doubleFactory);

        matrixCopy.setMatrix(invertMatrix);

        return matrixCopy;
    }

    public static AbstractDouble[][] invert(AbstractDouble a[][], AbstractDoubleFactory doubleFactory) {

        int n = a.length;

        AbstractDouble x[][] = new AbstractDouble[n][n];

        AbstractDouble b[][] = new AbstractDouble[n][n];

        initMatrixZero(x, doubleFactory);

        initMatrixZero(b, doubleFactory);

        int index[] = new int[n];

        for (int i = 0; i < n; ++i) {

            b[i][i] = doubleFactory.getNew(1.0);
        }

        // Transform the matrix into an upper triangle
        gaussian(a, index, doubleFactory);

        // Update the matrix b[i][j] with the ratios stored

        for (int i = 0; i < n - 1; ++i) {

            for (int j = i + 1; j < n; ++j) {

                for (int k = 0; k < n; ++k) {

                    b[index[j]][k] = b[index[j]][k].substract(a[index[j]][i].multiply(b[index[i]][k]));
                }
            }
        }

        // Perform backward substitutions

        for (int i = 0; i < n; ++i) {

            x[n - 1][i] = b[index[n - 1]][i].divide(a[index[n - 1]][n - 1]);

            for (int j = n - 2; j >= 0; --j) {

                x[j][i] = b[index[j]][i];

                for (int k = j + 1; k < n; ++k) {

                    x[j][i] = x[j][i].substract(a[index[j]][k].multiply(x[k][i]));
                }

                x[j][i] = x[j][i].divide(a[index[j]][j]);
            }
        }

        return x;
    }

    private static void initMatrixZero(AbstractDouble[][] x, AbstractDoubleFactory doubleFactory) {

        int rows = x.length, cols = x[0].length;

        for( int row = 0 ;  row < rows ; row ++){

            for( int col = 0 ;  col < cols ; col ++){

                x[row][col] = doubleFactory.getNew(0.0);
            }
        }
    }

    public static void gaussian(AbstractDouble a[][], int index[], AbstractDoubleFactory doubleFactory) {

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

                AbstractDouble c0 = a[i][j].abs();

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

                AbstractDouble pi0 = a[index[i]][j].abs();

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

                AbstractDouble pj = a[index[i]][j].divide( a[index[j]][j]);

                //Record pivoting ratios below the diagonal

                a[index[i]][j] = pj;

                //Modify other elements accordingly

                for (int l = j + 1; l < n; ++l) {

                    //a[index[i]][l] -= pj * a[index[j]][l];
                    a[index[i]][l] = a[index[i]][l].substract(pj.multiply(a[index[j]][l]));
                }
            }
        }
    }

}
