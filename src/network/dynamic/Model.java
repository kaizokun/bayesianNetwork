package network.dynamic;

import network.ProbabilityCompute;
import network.Variable;

import java.util.List;

public class Model {

    private List<Variable> dependencies;

    private ProbabilityCompute probabilityCompute;

    public Model(List<Variable> dependencies, ProbabilityCompute probabilityCompute) {

        this.dependencies = dependencies;

        this.probabilityCompute = probabilityCompute;
    }

    public List<Variable> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Variable> dependencies) {
        this.dependencies = dependencies;
    }

    public ProbabilityCompute getProbabilityCompute() {
        return probabilityCompute;
    }

    public void setProbabilityCompute(ProbabilityCompute probabilityCompute) {
        this.probabilityCompute = probabilityCompute;
    }
}
