package domain.data;

public class MyDouble implements AbstractDouble<MyDouble> {

    private Double value;

    public MyDouble(java.lang.Double value) {

        this.value = value;
    }

    @Override
    public AbstractDouble add(MyDouble value) {

        return new MyDouble(this.value + value.value );
    }

    @Override
    public AbstractDouble substract(MyDouble value) {

        return new MyDouble(this.value - value.value );
    }

    @Override
    public AbstractDouble multiply(MyDouble value) {

        return new MyDouble(this.value * value.value );
    }

    @Override
    public AbstractDouble divide(MyDouble value) {

        return new MyDouble(this.value / value.value );
    }

    @Override
    public String toString() {

        return this.value.toString();
    }

    @Override
    public int compareTo(MyDouble o) {

        return this.value.compareTo(o.value);
    }

    @Override
    public Double getDoubleValue() {
        return this.value;
    }

    @Override
    public AbstractDouble abs() {

        return new MyDouble(Math.abs(this.value));
    }

    @Override
    public AbstractDouble copy() {

        return new MyDouble(new Double(this.value));
    }
}
