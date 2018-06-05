package domain.data;

import java.math.BigDecimal;

public interface AbstractDoubleFactory {

    AbstractDouble getNew(Double value);

    AbstractDouble getNew(BigDecimal value);
}
