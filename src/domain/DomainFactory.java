package domain;

public class DomainFactory {

    public static IDomain getBooleanDomain(){

        return new Domain(1, 0);
    }

    public static IDomain getABCDDomain(){

        return new Domain('a', 'b', 'c', 'd');
    }
}
