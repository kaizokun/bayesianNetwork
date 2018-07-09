package domain;

import java.util.List;

public interface IDomain {
    
    List<Domain.DomainValue> getValues();

    int getSize();

    Domain.DomainValue getDomainValue(int j);

    Domain.DomainValue getDomainValue(Object object);
}
