package network.dynamic;

import domain.IDomain;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.ProbabilityCompute;
import network.Variable;

import java.util.*;

public class DynamicBayesianNetwork extends BayesianNetwork {

    protected int time;

    protected Map<Variable, Variable> lastVariables = new Hashtable<>();

    protected Map<Variable, Model> models = new Hashtable<>();

    public DynamicBayesianNetwork(AbstractDoubleFactory doubleFactory) {

        super(doubleFactory);
    }

    public void addLeaf(String label, IDomain domain, ProbabilityCompute probabilityCompute){

        Variable leaf = this.addRootVariable(label, domain, probabilityCompute);

        this.lastVariables.put(leaf, leaf);
    }

    public void addModel(Variable variable, Model model){

        this.models.put(variable, model);
    }

    public void extend(){

        this.time ++;

        List<Variable> newVars = new LinkedList<>();

        //ajoute des noeuds d'observations
        for(Variable variable : models.keySet()){

            Model model = models.get(variable);

            ArrayList<Variable> newDependencies = new ArrayList<>();

            for(Variable dependencie : model.getDependencies() ){

                newDependencies.add(lastVariables.get(dependencie));
            }

            Variable newVar = new Variable(variable.getLabel(), variable.getDomain(), model.getProbabilityCompute(), newDependencies);

            newVars.add(newVar);
        }

        //remplacement des observations précédentes
        for(Variable newVar : newVars){

            lastVariables.put(newVar, newVar);
        }
    }

}
