package network;

import domain.Domain;
import domain.data.AbstractDouble;

import java.util.List;

public interface ProbabilityCompute {

    AbstractDouble getProbability(Variable value);

    Domain.DomainValue getRandomValue(Variable var);
}
