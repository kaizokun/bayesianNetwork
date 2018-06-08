package network;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public interface ProbabilityCompute {

    AbstractDouble getProbability(Variable value);

    Domain.DomainValue getRandomValue(Variable var);

    void showCumulativeMarkovFrequencies();

    void initCumulativeMarkovFrequencies(Variable variable);

    Domain.DomainValue getValueFromMarkovCover(Variable variable);
}
