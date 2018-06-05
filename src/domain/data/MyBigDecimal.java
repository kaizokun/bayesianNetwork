package domain.data;

import java.math.BigDecimal;

public class MyBigDecimal implements AbstractDouble<MyBigDecimal>{

   private java.math.BigDecimal bigDecimal;

    public MyBigDecimal(java.math.BigDecimal bigDecimal) {

        this.bigDecimal = bigDecimal;
    }

    public MyBigDecimal(java.lang.Double d) {

        this.bigDecimal = new BigDecimal(d);
    }

    @Override
    public AbstractDouble add(MyBigDecimal value) {

        return new MyBigDecimal(this.bigDecimal.add(value.bigDecimal));
    }

    @Override
    public AbstractDouble substract(MyBigDecimal value) {

        return new MyBigDecimal(this.bigDecimal.subtract(value.bigDecimal));
    }

    @Override
    public AbstractDouble multiply(MyBigDecimal value) {

        return new MyBigDecimal(this.bigDecimal.multiply(value.bigDecimal));
    }

    @Override
    public AbstractDouble divide(MyBigDecimal value) {

        return new MyBigDecimal(this.bigDecimal.divide(value.bigDecimal));
    }

    @Override
    public String toString() {

        return this.bigDecimal.toString();
    }
}
