package decision;

import domain.Domain;
import math.Distribution;
import network.Variable;

import java.util.List;
import java.util.Set;

public interface PDMPO {

    List<Variable> getStates();

    List<Variable> getActions();

    Set<Domain.DomainValue> getActionsFromState(Distribution forward);
}
