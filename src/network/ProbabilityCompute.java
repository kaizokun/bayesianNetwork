package network;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;


public interface ProbabilityCompute {

    int size();

    AbstractDouble getProbability(Variable var);

    Domain.DomainValue getRandomValue(Variable var);

    void showCumulativeMarkovFrequencies();

    void initCulumativeFrequencies();

    void initCumulativeMarkovFrequencies(Variable variable);

    Domain.DomainValue getRandomValueFromMarkovCover(Variable variable);

    AbstractDoubleFactory getDoubleFactory();
}
