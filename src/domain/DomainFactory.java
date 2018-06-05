package domain;

public class DomainFactory {

    public static IDomain getBooleanDomain(){

        return new Domain(1, 0);
    }
}
