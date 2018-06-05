package network;

import domain.data.AbstractDouble;

import java.util.List;

public interface ProbabilityCompute {

    AbstractDouble getProbability(Variable value);
}
