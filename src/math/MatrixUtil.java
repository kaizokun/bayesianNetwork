package math;

import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

public class MatrixUtil {
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

        // Transform the tcp into an upper triangle
        gaussian(matrix, index, doubleFactory);

        // Update the tcp b[i][j] with the ratios stored

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
