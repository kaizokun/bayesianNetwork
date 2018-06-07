package network;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.Hashtable;
import java.util.List;

public interface ProbabilityCompute {

    AbstractDouble getProbability(Variable value);

    Domain.DomainValue getRandomValue(Variable var);

    AbstractDoubleFactory getDoubleFactory();

    Hashtable<String, Hashtable<Domain.DomainValue, AbstractDouble>> getTCP();
}
