package network;

import domain.Domain;
import domain.IDomain;
import domain.MegaDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import math.Combination;

import java.util.*;

public class MegaVariable extends Variable implements Iterable<Variable> {

    protected List<Variable> compoVars;

    protected AbstractDoubleFactory doubleFactory;

    /**
     * Generate a mega variable from a list of variables
     * must be sorted by label (asc)
     * and a specific time.
     * <p>
     * each variable is copied for a futur domainValue assignation.
     * (label, time, domainValue, domain)
     */

    public MegaVariable(){}

    public MegaVariable(Collection<Variable> compoVars, int time, AbstractDoubleFactory doubleFactory) {

        this(compoVars, time, initDomain(compoVars));

        this.doubleFactory = doubleFactory;
    }

    public MegaVariable(Collection<Variable> compoVars, int time, IDomain domain) {

        this.time = time;

        this.compoVars = new ArrayList<>(compoVars.size());

        for (Variable compoVar : compoVars) {

            Variable compoVarCopy = new Variable();

            compoVarCopy.domainValue = compoVar.domainValue;

            compoVarCopy.label = compoVar.label;

            compoVarCopy.domain = compoVar.domain;

            compoVarCopy.probabilityCompute = compoVar.probabilityCompute;

            compoVarCopy.dependencies = compoVar.dependencies;

            this.compoVars.add(compoVarCopy);
        }

        this.label = getLabel();

        this.domain = domain;
    }

    private static IDomain initDomain(Collection<Variable> compoVars) {

        List<List<Domain.DomainValue>> domainValuesList = new ArrayList<>(compoVars.size());

        for (Variable variable : compoVars) {

            domainValuesList.add(variable.getDomainValues());
        }

        List<List<Domain.DomainValue>> combinations = Combination.getCombinations(domainValuesList);

        return new MegaDomain(combinations);
    }

    public List<Domain.DomainValue> getDomainValuesCheckInit(){

        return getDomainValuesCheckInit(this.getCompoVars());
    }

    protected static List<Domain.DomainValue> getDomainValuesCheckInit(Collection<Variable> compoVars) {

        List<List<Domain.DomainValue>> domainValuesList = new ArrayList<>(compoVars.size());

        for (Variable variable : compoVars) {

            if(variable.isInit()){

                domainValuesList.add(Arrays.asList(new Domain.DomainValue[]{variable.getDomainValue()}));

            }else {

                domainValuesList.add(variable.getDomainValues());
            }
        }

        List<List<Domain.DomainValue>> combinations = Combination.getCombinations(domainValuesList);

        List<Domain.DomainValue> combinations2 = new ArrayList<>(combinations.size());

        for(List<Domain.DomainValue> values : combinations){

            combinations2.add(new MegaDomain.MegaDomainValue(values));
        }

        return combinations2;
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

        List<Domain.DomainValue> domainValues = new ArrayList<>(this.compoVars.size());

        for (Variable variable : this.compoVars) {

            domainValues.add(variable.getDomainValue());
        }

        return domainValues;
    }

    @Override
    public void setDomainValue(Domain.DomainValue value) {

        Iterator<Domain.DomainValue> domainValueIterator = value.iterator();

        Iterator<Variable> variableIterator = compoVars.iterator();

        while (domainValueIterator.hasNext() && variableIterator.hasNext()) {

            variableIterator.next().setDomainValue(domainValueIterator.next());
        }
    }

    @Override
    public Domain.DomainValue getDomainValue() {

       return this.domain.getDomainValue(this.getMegaVarValues());
    }

    @Override
    public Domain.DomainValue saveDomainValue() {

        return new MegaDomain.MegaDomainValue(this.getMegaVarValues());
    }

    @Override
    public AbstractDouble getProbabilityForCurrentValue() {

        AbstractDouble prob = this.doubleFactory.getNew(1.0);

        for (Variable variable : this.compoVars) {

            prob = prob.multiply(variable.getProbabilityForCurrentValue());
        }

        return prob;
    }

    @Override
    public String getVarTimeId() {

        StringBuilder keybuilder = new StringBuilder();

        for (Variable variable : compoVars) {

            keybuilder.append(variable.getVarTimeId());

            keybuilder.append('.');
        }

        keybuilder.deleteCharAt(keybuilder.length() - 1);

        return keybuilder.toString();
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

    public void setCompoVars(List<Variable> compoVars) {
        this.compoVars = compoVars;
    }

    @Override
    public String toString() {

        return this.label + "_" + this.time + " = " + this.domainValue + ((this.compoVars != null && !this.compoVars.isEmpty()) ? " - COMPOSITION : " + this.compoVars : "");
    }

    @Override
    public Iterator<Variable> iterator() {

        return this.compoVars.iterator();
    }

    @Override
    public Variable mmcCopy(int time) {

        return new MegaVariable(this.compoVars, time, this.getDomain());
    }

    public static Variable encapsulate(List<Variable> variables){

        if(variables.size() > 1) {

            MegaVariable megavariable = new MegaVariable();

            megavariable.setDomain(initDomain(variables));

            megavariable.setCompoVars(variables);

            return megavariable;

        }else{

            return variables.get(0);
        }
    }
}
