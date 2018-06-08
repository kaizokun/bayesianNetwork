package network;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;


public interface ProbabilityCompute {

    AbstractDouble getProbability(Variable value);

    Domain.DomainValue getRandomValue(Variable var);

    void showCumulativeMarkovFrequencies();

    void initCumulativeMarkovFrequencies(Variable variable);

    Domain.DomainValue getRandomValueFromMarkovCover(Variable variable);

    AbstractDoubleFactory getDoubleFactory();
}
