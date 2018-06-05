package domain.data;

import java.math.BigDecimal;

public class MyDoubleFactory implements AbstractDoubleFactory {

    @Override
    public AbstractDouble getNew(Double value) {

        return new MyDouble(value);
    }

    @Override
    public AbstractDouble getNew(BigDecimal value) {

        return new MyDouble(value.doubleValue());
    }
}
