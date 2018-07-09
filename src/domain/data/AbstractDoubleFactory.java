package domain.data;

import java.math.BigDecimal;

public interface AbstractDoubleFactory {

    AbstractDouble getNew(Double value);

    AbstractDouble getNew(BigDecimal value);

    static AbstractDouble[][] convert(Double[][] entries, AbstractDoubleFactory factory) {

        AbstractDouble[][] matrix = new AbstractDouble[entries.length][entries[0].length];

        int row = 0;

        for (Double[] entry : entries) {

            int col = 0;

            for (Double value : entry) {

                matrix[row][col] = factory.getNew(value);

                col++;
            }

            row++;
        }

        return matrix;
    }
}
