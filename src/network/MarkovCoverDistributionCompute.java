package network;

import domain.Domain;

import java.util.List;

public interface MarkovCoverDistributionCompute {

    void initCumulativeMarkovFrequencies(List<Variable> obs, Variable variable);

    Domain.DomainValue getRandomValueFromMarkovCover(Variable variable);
}
