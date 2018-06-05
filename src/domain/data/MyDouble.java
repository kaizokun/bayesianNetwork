package domain.data;

public class MyDouble implements AbstractDouble<MyDouble> {

    private java.lang.Double value;

    public MyDouble(java.lang.Double value) {

        this.value = value;
    }

    @Override
    public AbstractDouble add(MyDouble value) {

        this.value += value.value;

        return this;
    }

    @Override
    public AbstractDouble substract(MyDouble value) {

        this.value -= value.value;

        return this;
    }

    @Override
    public AbstractDouble multiply(MyDouble value) {

        this.value *= value.value;

        return this;
    }

    @Override
    public AbstractDouble divide(MyDouble value) {

        return new MyDouble(this.value / value.value );
    }

    @Override
    public String toString() {

        return this.value.toString();
    }
}
