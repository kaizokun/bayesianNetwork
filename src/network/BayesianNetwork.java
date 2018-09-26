package network;

import domain.IDomain;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class BayesianNetwork {

    public BayesianNetwork() { }

    public BayesianNetwork(AbstractDoubleFactory doubleFactory) {

        this.doubleFactory = doubleFactory;
    }

    protected AbstractDoubleFactory doubleFactory;

    protected List<Variable> roots = new ArrayList<>();

    protected Hashtable<String, Variable> variables = new Hashtable<>();

    public ProbabilityCompute getTCP(IDomain booleanDomain, Double[][] doubles) {

        return new ProbabilityComputeFromTCP(booleanDomain, doubles, this.doubleFactory);
    }

    public ProbabilityCompute getTCP(List<Variable> dependencies, IDomain varDom, Double[][] entries) {

        ProbabilityComputeFromTCP tcp = new ProbabilityComputeFromTCP(dependencies, varDom, entries, this.doubleFactory);

        // tcp.initCulumativeFrequencies();

        return tcp;
    }

    public void addVariable(Variable variable) {

        this.variables.put(variable.getLabel(), variable);

        if (variable.isRoot()) {

            this.roots.add(variable);
        }
    }

    public Variable addRootVariable(String label, IDomain domain, ProbabilityCompute probabilityCompute) {

        return addRootVariable(label, domain, probabilityCompute, 0);
    }

    public Variable addRootVariable(String label, IDomain domain, ProbabilityCompute probabilityCompute, int time) {

        Variable newVar = new Variable(label, domain, probabilityCompute, time);

        this.addVariable(newVar);

        return newVar;
    }

    public Variable addVariable(String label, IDomain domain, ProbabilityCompute probabilityCompute, List<Variable> dependencies) {

        Variable newVar = new Variable(label, domain, probabilityCompute, dependencies);

        this.addVariable(newVar);

        return newVar;
    }


    //marque les variables importante pour la requete soit les observations les requetes
    //et leur ancetres
    public static void markImportantVars(List<Variable> request, List<Variable> observations) {

        List<Variable> checkParents = new LinkedList<>();

        checkParents.addAll(observations);

        checkParents.addAll(request);

        markImportant(checkParents);

    }

    private static void markImportant(List<Variable> vars) {

        for (Variable var : vars) {

            if (!var.isImportant()) {

                var.setImportant(true);

                markImportant(var.getDependencies());
            }
        }
    }

    public LinkedList<Variable> getTopologicalOrder() {

        //list des noeuds racines
        LinkedList<Variable> vars = new LinkedList<>();
        //ajute les noeuds racines à la liste finale
        LinkedList<Variable> orderedVars = new LinkedList<>();

        for (Variable root : this.roots) {

            if (root.isImportant()) {

                vars.add(root);

                orderedVars.add(root);
            }
        }

        //int d = 0;

        //tant qu'il y a des noeuds à traiter
        while (!vars.isEmpty()) {
            //créer une liste contenant les noeuds de niveau inférieur
            LinkedList<Variable> children = new LinkedList<>();
            //pour chaque variable du niveau superieur
            for (Variable var : vars) {
                //ajoute chaque variable du niveau inférieur dans la liste
                for (Variable child : var.getChildren()) {
                    //marque les noueuds traités
                    if (!child.isAdded() && child.isImportant()) {

                        child.setAdded(true);

                        children.add(child);
                    }

                    //child.setDepth(d);
                }
            }
            //supprime les variables du niveau supérieur
            vars.clear();
            //ajoute celles du niveau inférieur
            vars.addAll(children);
            //ainqi que dans la liste finale
            orderedVars.addAll(children);
/*
            for(network.Variable variable : orderedVars){
                variable.setAdded(false);
            }
*/

            // d++;
        }

        return orderedVars;
    }

    public Hashtable<String, Variable> getVariables() {

        return variables;
    }

    public Variable getVariable(String label) {

        return this.variables.get(label);
    }

    public AbstractDoubleFactory getDoubleFactory() {

        return doubleFactory;
    }

}
