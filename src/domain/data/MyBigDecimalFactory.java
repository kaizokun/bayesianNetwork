package domain.data;

import java.math.BigDecimal;

public class MyBigDecimalFactory implements AbstractDoubleFactory {

    @Override
    public AbstractDouble getNew(Double value) {
        return new MyBigDecimal(value);
    }

    @Override
    public AbstractDouble getNew(BigDecimal value) {
        return new MyBigDecimal(value);
    }

    @Override
    public AbstractDouble getNew(Double value, int scale) {
        return new MyBigDecimal(value, scale);
    }

    @Override
    public AbstractDouble getNew(BigDecimal value, int scale) {
        return new MyBigDecimal(value, scale);
    }
}
