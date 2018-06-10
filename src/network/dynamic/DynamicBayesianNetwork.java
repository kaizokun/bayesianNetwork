package network.dynamic;

import domain.IDomain;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.ProbabilityCompute;
import network.Variable;

import java.util.*;

public class DynamicBayesianNetwork extends BayesianNetwork {

    protected int time;

    protected Map<Variable, Variable> lastStates = new Hashtable<>();

    protected Map<Variable, Variable> lastObservations = new Hashtable<>();

    protected Map<Variable, Model> transitionModels = new Hashtable<>();

    protected Map<Variable, Model> captorModels = new Hashtable<>();

    public DynamicBayesianNetwork(AbstractDoubleFactory doubleFactory) {

        super(doubleFactory);
    }

    public void addLeaf(String label, IDomain domain, ProbabilityCompute probabilityCompute){

        Variable leaf = this.addRootVariable(label, domain, probabilityCompute);

        this.lastStates.put(leaf, leaf);
    }

    public void addTransitionModel(Variable variable, Model model){

        this.transitionModels.put(variable, model);
    }

    public void addCaptorModel(Variable variable, Model model){

        this.captorModels.put(variable, model);
    }

    public void extend(){

        this.time ++;

        //transitions
        extend(this.lastStates, this.lastStates, this.transitionModels);
        //capteurs
        extend(this.lastStates, this.lastObservations, this.transitionModels);
    }

    private void extend( Map<Variable, Variable> leafs, Map<Variable, Variable> leafs2, Map<Variable, Model> models){

        List<Variable> newVars = new LinkedList<>();

        //ajoute des noeuds d'observations
        for(Variable variable : models.keySet()){

            Model model = models.get(variable);

            ArrayList<Variable> newDependencies = new ArrayList<>();

            for(Variable dependencie : model.getDependencies() ){

                newDependencies.add(leafs.get(dependencie));
            }

            Variable newVar = new Variable(variable.getLabel(), variable.getDomain(), model.getProbabilityCompute(), newDependencies);

            newVars.add(newVar);
        }

        //remplacement des observations précédentes
        for(Variable newVar : newVars){

            leafs2.put(newVar, newVar);
        }
    }

}
