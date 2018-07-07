package environment;


public interface Percept<T> {

    boolean match(T percept);

    Object getValue();
}
