package network.dynamic;

import network.ProbabilityCompute;
import network.Variable;

import java.util.LinkedList;
import java.util.List;

public class Model {

    private int markovOrder;

    private List<Dependency> dependencies;

    private ProbabilityCompute probabilityCompute;

    public Model(ProbabilityCompute probabilityCompute) {

        this.probabilityCompute = probabilityCompute;

        this.dependencies = new LinkedList<>();
    }

    public void addDependencies(Variable... variables) {

        for (Variable variable : variables) {

            this.addDependencie(variable, 1);
        }
    }

    public void addDependencie(Variable variable) {

        this.addDependencie(variable, 1);
    }

    public void addDependencie(Variable variable, int markovOrder) {

        this.dependencies.add(new Dependency(variable, markovOrder));
    }

    public List<Dependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    public ProbabilityCompute getProbabilityCompute() {
        return probabilityCompute;
    }

    public void setProbabilityCompute(ProbabilityCompute probabilityCompute) {
        this.probabilityCompute = probabilityCompute;
    }

    public int getMarkovOrder() {
        return markovOrder;
    }

    public void setMarkovOrder(int markovOrder) {
        this.markovOrder = markovOrder;
    }

    public static class Dependency {

        private Variable dependency;

        private int markovOrder;

        public Dependency(Variable dependency, int markovOrder) {

            this.dependency = dependency;

            this.markovOrder = markovOrder;
        }

        public Variable getDependency() {
            return dependency;
        }

        public void setDependency(Variable dependency) {
            this.dependency = dependency;
        }

        public int getMarkovOrder() {
            return markovOrder;
        }

        public void setMarkovOrder(int markovOrder) {
            this.markovOrder = markovOrder;
        }
    }
}
