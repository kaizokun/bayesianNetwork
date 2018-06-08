package network;

import domain.Domain;

import java.util.List;

public class MarkovCoverDistributionComputeStatic implements MarkovCoverDistributionCompute {

    @Override
    public void initCumulativeMarkovFrequencies(List<Variable> obs, Variable variable) {

        variable.loadMarkovCover(obs);

        variable.probabilityCompute.initCumulativeMarkovFrequencies(variable);
    }

    @Override
    public Domain.DomainValue getRandomValueFromMarkovCover(Variable variable) {

        return  variable.probabilityCompute.getRandomValueFromMarkovCover(variable);
    }
}
