package decision;

import domain.Domain;
import domain.data.AbstractDouble;
import math.Distribution;
import network.Variable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public interface PDMPO {

    AbstractDouble getUtility(Distribution forward);

    List<Variable> getStates();

    List<Variable> getActions();

    Variable getActionVar();

    Variable getPerceptVar();

    Set<Domain.DomainValue> getActionsFromState(Distribution forward, AbstractDouble minProb);

    Collection<RsState> getResultStates(Domain.DomainValue state, Domain.DomainValue action);

    Domain.DomainValue getPerceptFromState(Domain.DomainValue state);

    boolean isFinalState(Domain.DomainValue state);

    class RsState {

        private Domain.DomainValue state;

        private AbstractDouble prob;

        public RsState(Domain.DomainValue state, AbstractDouble prob) {
            this.state = state;
            this.prob = prob;
        }

        public Domain.DomainValue getState() {
            return state;
        }

        public void setState(Domain.DomainValue state) {
            this.state = state;
        }

        public AbstractDouble getProb() {
            return prob;
        }

        public void setProb(AbstractDouble prob) {
            this.prob = prob;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RsState)) return false;
            RsState rsState = (RsState) o;
            return Objects.equals(getState(), rsState.getState());
        }

        @Override
        public int hashCode() {

            return Objects.hash(getState());
        }
    }
}
