package domain;

import java.util.List;

public interface IDomain {
    
    List<Domain.DomainValue> getValues();

    Object getObjectValue(int j);

    int getSize();

    Object getValue(int j);

    Domain.DomainValue getDomainValue(Object object);
}
