package network.dynamic;

import domain.IDomain;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.Model.Dependency;

import java.util.*;

public class DynamicBayesianNetwork extends BayesianNetwork {

    protected int time;

    protected Map<Variable, Variable> lastTimeVars = new Hashtable<>();

    protected Map<Variable, List<Model>> models = new Hashtable<>();

    public DynamicBayesianNetwork(AbstractDoubleFactory doubleFactory) {

        super(doubleFactory);
    }

    public void addLeaf(String label, IDomain domain, ProbabilityCompute probabilityCompute) {

        Variable leaf = this.addRootVariable(label, domain, probabilityCompute);

        this.lastTimeVars.put(leaf, leaf);
    }

    /**
     * ! les modeles doivent être ajoutés dans l'ordre de temps d'utilisation
     */
    public void addModel(Variable variable, Model model, int time) {
        //recupere la liste des modeles par limite de temps atteinte pour une variable
        List<Model> varModels = this.models.get(variable);

        //crée et ajoute la liste si elle n'existe pas
        if (varModels == null) {

            varModels = new ArrayList<>();

            this.models.put(variable, varModels);
        }

        varModels.add(model);
    }

    /**
     * ! ou avec cette méthode l'ordre croissant n'est pas obligatoire
     * mais obliger d'indiquer le maximum de modele pour une variable
     */
    public void addModel(Variable variable, Model model, int time, int maxModels) {
        //recupere la liste des modeles par limite de temps atteinte pour une variable
        List<Model> varModels = this.models.get(variable);

        //crée et ajoute la liste si elle n'existe pas
        if (varModels == null) {

            varModels = new ArrayList<>(Arrays.asList(new Model[maxModels]));

            this.models.put(variable, varModels);
        }

        varModels.set(time, model);
    }


    private void addDeeperDependencies(Variable lastDep, ArrayList<Variable> newDependencies, int limit) {

        if (limit > 0) {

            Variable depperDep = lastDep.getParent(lastDep, lastDep.getTime() - 1);

            newDependencies.add(depperDep);

            addDeeperDependencies(depperDep, newDependencies, limit - 1);
        }
    }

    public void extend() {

        this.time++;

        List<Variable> newVars = new LinkedList<>();

        //extension du reseau pour chaque model d'extension
        for (Variable variable : models.keySet()) {
            //récupere la liste des modeles d'extension pour une variable
            //en fonction du temps
            List<Model> varModels = this.models.get(variable);

            Model model;

            //si le temps est inferieur ou egal au nombre de modele de la variable
            if (this.time <= varModels.size()) {
                //récupere le modele correspondant au temps courant
                //un modele d'ordre de markov d'ordre trois serait limité à deux au temps 2 par exemple
                //dependences et TCP differente
                model = varModels.get(this.time - 1);

            } else {
                //sinon recupere le dernier modele celui le plus complet, le nombre de dependences étant suffisantes
                model = varModels.get(varModels.size() - 1);
            }

            ArrayList<Variable> newDependencies = new ArrayList<>();

            for (Dependency dependencie : model.getDependencies()) {

                Variable lastDep = this.lastTimeVars.get(dependencie.getDependency());

                newDependencies.add(lastDep);
                //récuperer les variables precedentes parmis les parents de la derniere dependance
                //si l'ordre est de 1 il ne se passe rien si supérieur à 1 on recupere
                //les mêmes variable de temps inférieur jusqu'à la limite d'ordre
                //(1)
                this.addDeeperDependencies(lastDep, newDependencies, dependencie.getMarkovOrder() - 1);
            }

            Variable newVar = new Variable(variable.getLabel(), variable.getDomain(), model.getProbabilityCompute(), newDependencies, this.time);

            newVars.add(newVar);
        }

        //remplacement des observations précédentes
        for (Variable newVar : newVars) {

            this.lastTimeVars.put(newVar, newVar);
        }

        //(1)
        //cependant peut poser problème en cas d'ancetre insuffisant
        //par exemple une variable qui aurait un modele de transition d'ordre 2 par exemple
        //lors du deploiement de la variable t1 il ne pourrait récuperer q'un seul parent
        //en t0 par consequent il faudrait une TCP pour ces cas particulier
        //la solution est d'avoir des modeles de transition differents
        //avec des ordres différents et des TCP differentes pour les differents temps
        //en tout cas au début jusqu'à atteindre un temps qu permette d'avoir les
        //dependances completes

    }

}
