package network;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.Hashtable;


public interface ProbabilityCompute {

    int size();

    String getValueKey(Domain.DomainValue value);

    AbstractDouble getProbability(Variable var);

    Domain.DomainValue getRandomValue(Variable var);

    void showCumulativeMarkovFrequencies();

    void initCulumativeFrequencies();

    void initCumulativeMarkovFrequencies(Variable variable);

    Domain.DomainValue getRandomValueFromMarkovCover(Variable variable);

    AbstractDoubleFactory getDoubleFactory();

    Hashtable<String, Hashtable<Domain.DomainValue, AbstractDouble>> getTCP();
}
