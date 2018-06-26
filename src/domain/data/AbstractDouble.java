package domain.data;

public interface AbstractDouble<Type> extends Comparable<Type> {


    AbstractDouble add(Type value);

    AbstractDouble substract(Type value);

    AbstractDouble multiply(Type value);

    AbstractDouble divide(Type value);

    Double getDoubleValue();

}
