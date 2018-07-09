package network;

import domain.Domain;
import domain.MegaDomain;
import domain.data.AbstractDouble;
import math.Combination;

import java.util.*;

public class MegaVariable extends Variable implements Iterable<Variable> {


    protected List<Variable> compoVars;

    /**
     * Generate a mega variable from a list of variables
     * must be sorted by label (asc)
     * and a specific time.
     * <p>
     * each variable is copied for a futur domainValue assignation.
     * (label, time, domainValue, domain)
     */

    public MegaVariable(Variable megaVariableObs, int time) {

        this(((MegaVariable) megaVariableObs).compoVars, time);
    }

    public MegaVariable(List<Variable> compoVars, int time) {

        this.time = time;

        this.compoVars = new ArrayList<>(compoVars.size());

        for (Variable compoVar : compoVars) {

            this.compoVars.add(new Variable(compoVar.label, this.time, compoVar.domainValue, compoVar.domain));
        }

        this.label = getLabel();

        this.initDomain();
    }

    private void initDomain() {

        List<List<Domain.DomainValue>> domainValuesList = new ArrayList<>(this.compoVars.size());

        for (Variable variable : this.compoVars) {

            domainValuesList.add(variable.getDomainValues());
        }

        List<List<Domain.DomainValue>> combinations = Combination.getCombinations(domainValuesList);

        this.domain = new MegaDomain(combinations);
    }

    public String getLabel() {

        if (label == null) {

            StringBuilder labelBuilder = new StringBuilder();

            for (Variable variable : this.compoVars) {

                labelBuilder.append(variable.getLabel() + "-");
            }

            labelBuilder.deleteCharAt(labelBuilder.length() - 1);

            label = labelBuilder.toString();
        }

        return label;
    }

    /**
     * initialize the variables composing a megavariable
     * from a list of domain values
     * the order of the values must match with the variables one
     * sorted by label ASC
     */
    public void setDomainValues(Object[] objects) {

        Iterator<Variable> variableIterator = this.compoVars.iterator();

        for (Object object : objects) {

            variableIterator.next().setDomainValue(new Domain.DomainValue(object));
        }
    }

    /**
     * initialize the variables composing a megavariable
     * from another list of variables already initialized
     * the order of the variables doesn't matter, they are sorted
     * inside the method
     */
    public void setDomainValuesFromVariables(Variable... variables) {

        Arrays.sort(variables, varLabelComparator);

        Iterator<Variable> variableIterator = this.compoVars.iterator();

        for (Variable variable : variables) {

            variableIterator.next().setDomainValue(variable.getDomainValue());
        }
    }

    /**
     * domainValue list of the variables composing the mega variable
     */
    public List<Domain.DomainValue> getMegaVarValues() {

        List<Domain.DomainValue> domainValues = new LinkedList<>();

        for (Variable variable : this.compoVars) {

            domainValues.add(variable.getDomainValue());
        }

        return domainValues;
    }

    @Override
    public void setDomainValue(Domain.DomainValue value) {

       Iterator<Domain.DomainValue> domainValueIterator = value.iterator();

        Iterator<Variable> variableIterator = compoVars.iterator();

        while (domainValueIterator.hasNext() && variableIterator.hasNext()){

            variableIterator.next().setDomainValue(domainValueIterator.next());
        }
    }

    @Override
    public AbstractDouble getProbabilityForCurrentValue() {

        AbstractDouble prob = this.probabilityCompute.getDoubleFactory().getNew(1.0);

        for(Variable variable : this.compoVars){

            prob = prob.multiply(variable.getProbabilityForCurrentValue());
        }

        return prob;
    }

    @Override
    public List<Domain.DomainValue> getDomainValues() {

        return this.domain.getValues();
    }

    public String getValueKey() {

        return this.getMegaVarValues().toString();
    }

    public List<Variable> getCompoVars() {
        return compoVars;
    }

    @Override
    public String toString() {

        return this.label + "_" + this.time + " = " + this.domainValue + ((this.compoVars != null && !this.compoVars.isEmpty()) ? " - COMPOSITION : " + this.compoVars : "");
    }

    @Override
    public Iterator<Variable> iterator() {

        return this.compoVars.iterator();
    }
}
