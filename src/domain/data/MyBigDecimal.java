package domain.data;

import java.math.BigDecimal;

public class MyBigDecimal implements AbstractDouble<MyBigDecimal>{

   private BigDecimal bigDecimal;

    public MyBigDecimal(BigDecimal bigDecimal) {

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

        return new MyBigDecimal(this.bigDecimal.divide(value.bigDecimal,6, BigDecimal.ROUND_HALF_UP));
    }

    @Override
    public String toString() {

        return this.bigDecimal.toString();
    }

    @Override
    public int compareTo(MyBigDecimal o) {
        return this.bigDecimal.compareTo(o.bigDecimal);
    }

    @Override
    public Double getDoubleValue() {

        return this.bigDecimal.doubleValue();
    }

    @Override
    public AbstractDouble abs() {

        return new MyBigDecimal(this.bigDecimal.abs());
    }

    @Override
    public AbstractDouble copy() {

        return new MyBigDecimal(new BigDecimal(this.bigDecimal.doubleValue()));
    }
}
