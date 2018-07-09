package network;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import math.Combination;
import math.TCP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MatrixTCP implements ProbabilityCompute {

    protected AbstractDoubleFactory doubleFactory;

    protected TCP tcp;

    public MatrixTCP(Variable[] dependencies, Variable variable, Double[][] entries, AbstractDoubleFactory doubleFactory) {

        this(Arrays.asList(dependencies), variable, entries, doubleFactory);
    }

    public MatrixTCP(List<Variable> dependencies, Variable variable, Double[][] entries, AbstractDoubleFactory doubleFactory) {

        this.doubleFactory = doubleFactory;

        this.init(dependencies, variable, entries);
    }

    private void init(List<Variable> dependencies, Variable variable, Double[][] entries) {

        List<List<Domain.DomainValue>> dependenciesDomainValues = new ArrayList<>(dependencies.size());

        List<List<Domain.DomainValue>> variableDomainValues = new ArrayList<>(1);

        for (Variable dependency : dependencies) {

            dependenciesDomainValues.add(dependency.getDomain().getValues());
        }

        variableDomainValues.add(variable.getDomain().getValues());

        List<List<Domain.DomainValue>> dependenciesDomainValuesCombinations = Combination.getCombinations(dependenciesDomainValues);

        List<List<Domain.DomainValue>> variableDomainValuesCombinations = Combination.getCombinations(variableDomainValues);

        AbstractDouble[][] tcp = AbstractDoubleFactory.convert(entries, doubleFactory);

        this.tcp = new TCP(tcp,
                dependencies, dependenciesDomainValuesCombinations,
                Arrays.asList(new Variable[]{variable}), variableDomainValuesCombinations,
                doubleFactory);
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public AbstractDouble getProbability(Variable var) {

        List<Domain.DomainValue> parentValues = new ArrayList<>(var.getDependencies().size());

        for (Variable dependency : var.getDependencies()) {

            parentValues.add(dependency.getDomainValue());
        }

        return this.tcp.getProbability(parentValues, Arrays.asList(new Domain.DomainValue[]{var.getDomainValue()}));
    }

    @Override
    public Domain.DomainValue getRandomValue(Variable var) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void showCumulativeMarkovFrequencies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initCulumativeFrequencies() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void initCumulativeMarkovFrequencies(Variable variable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Domain.DomainValue getRandomValueFromMarkovCover(Variable variable) {

        throw new UnsupportedOperationException();
    }

    @Override
    public AbstractDoubleFactory getDoubleFactory() {
        return doubleFactory;
    }
}
