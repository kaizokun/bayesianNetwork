package network.dynamic;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDoubleFactory;
import inference.dynamic.Forward;
import math.Distribution;
import math.Matrix;
import network.BayesianNetwork;
import network.MegaVariable;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.Model.Dependency;

import java.util.*;

import static inference.dynamic.Util.getDistribSavedKey;
import static inference.dynamic.Util.getTreeIdent;

public class DynamicBayesianNetwork extends BayesianNetwork {

    protected int time, initTime;

    protected Map<Integer, Map<Variable, Variable>> timeVariables = new Hashtable<>();

    protected Map<Variable, List<Model>> transitionModels = new Hashtable<>();

    protected Map<Variable, List<Model>> captorsModels = new Hashtable<>();

    protected Forward forward;

    protected Map.Entry<Integer, Matrix> lastForward, lastMax;

    protected List<Variable> stateVariables, observationVariables;

    public DynamicBayesianNetwork(AbstractDoubleFactory doubleFactory, Forward forward, int time) {

        this(doubleFactory, time);

        this.forward = forward;
    }

    public DynamicBayesianNetwork(AbstractDoubleFactory doubleFactory, int time) {

        super(doubleFactory);

        this.initTime = time;

        this.time = time;
    }

    public DynamicBayesianNetwork(AbstractDoubleFactory doubleFactory) {

        this(doubleFactory, 0);
    }

    public Variable addRootVariable(String label, IDomain domain, ProbabilityCompute probabilityCompute) {

        Variable root = super.addRootVariable(label, domain, probabilityCompute, this.initTime);

        this.getTimeVariables(this.initTime).put(root, root);

        return root;
    }

    public Map<Variable, Variable> getTimeVariables(int time) {

        Map<Variable, Variable> variables = this.timeVariables.get(time);

        if (variables == null && time <= this.time) {

            variables = new Hashtable<>();

            this.timeVariables.put(time, variables);
        }

        return variables;
    }

    public Variable getVariable(int time, Variable variable) {

        //System.out.println(time+" GET VAR : "+this.timeVariables.get(time)+"\n var : "+variable);

        return this.getTimeVariables(time).get(variable);
    }

    /**
     * ! les modeles doivent être ajoutés dans l'ordre de temps d'utilisation
     */
    public void addTransitionModel(Variable variable, Model model) {
        //recupere la liste des modeles par limite de temps atteinte pour une variable
        addModel(variable, model, transitionModels);
    }

    public void addCaptorModel(Variable variable, Model model) {
        //recupere la liste des modeles par limite de temps atteinte pour une variable
        addModel(variable, model, captorsModels);
    }

    private void addModel(Variable variable, Model model, Map<Variable, List<Model>> models) {

        List<Model> varModels = models.get(variable);

        //crée et ajoute la liste si elle n'existe pas
        if (varModels == null) {

            varModels = new ArrayList<>();

            models.put(variable, varModels);
        }

        varModels.add(model);
    }

    /**
     * ! ou avec cette méthode l'ordre croissant n'est pas obligatoire
     * mais obliger d'indiquer le maximum de modele pour une variable
     */

    private void addDeeperDependencies(Variable lastDep, LinkedList<Variable> timeDependencies, int limit) {

        if (limit > 0) {

            Variable depperDep = lastDep.getParent(lastDep.getTime() - 1);
            //ajoute les dependances plus lointaine dans le temps en premier
            timeDependencies.addFirst(depperDep);

            addDeeperDependencies(depperDep, timeDependencies, limit - 1);
        }
    }

    public List<Variable> getLastStateVariables() {

        return getLastStateVariables(this.getTime());
    }

    public List<Variable> getLastStateVariables(int time) {

        return getLastVariables(time, this.transitionModels.keySet());
    }

    public List<Variable> getLastObservationVariables() {

        return getLastObservationVariables(this.getTime());
    }

    public List<Variable> getLastObservationVariables(int time) {

        return getLastVariables(time, this.captorsModels.keySet());
    }

    private List<Variable> getLastVariables(int time, Set<Variable> varSet) {

        List<Variable> variables = new ArrayList<>();

        for (Variable variable : varSet) {

            variables.add(this.getVariable(time, variable));
        }

        return variables;
    }

    public Variable getMegaState() {
        return MegaVariable.encapsulate(this.getLastStateVariables());
    }

    public Variable getMegaState(int time) {
        return MegaVariable.encapsulate(this.getLastStateVariables(time));
    }

    public Variable getMegaObs() {

        return MegaVariable.encapsulate(this.getLastObservationVariables());
    }

    public Variable getMegaObs(int time) {

        return MegaVariable.encapsulate(this.getLastObservationVariables(time));
    }


    public Variable getMegaState(Variable megaState) {
        return MegaVariable.encapsulate(megaState, this.getLastStateVariables());
    }

    public Variable getMegaState(Variable megaState, int time) {
        return MegaVariable.encapsulate(megaState, this.getLastStateVariables(time));
    }

    public Variable getMegaObs(Variable megaState) {

        return MegaVariable.encapsulate(megaState, this.getLastObservationVariables());
    }

    public Variable getMegaObs(Variable megaState, int time) {

        return MegaVariable.encapsulate(megaState, this.getLastObservationVariables(time));
    }

    public void extend(Variable... variables) {

        this.extend();

        for (Variable variable : variables) {

            this.getVariable(this.time, variable).setDomainValue(variable.getDomainValue());
        }

        if (forward != null) {

            Variable megaState = getMegaState();

            Matrix lastForward = this.lastForward != null ? this.lastForward.getValue() : null;

            Matrix lastMax = this.lastMax != null ? this.lastMax.getValue() : null;

            Forward.ForwardMax forwardMax = this.forward.forward(megaState, getMegaObs(), lastForward, lastMax);

            this.setLastForward(forwardMax.getForward());

            this.setLastMax(forwardMax.getMax());

            if (this.getTime() > this.getInitTime()) {

                System.out.println(this.getTime() + " " + this.getInitTime() + " " + megaState);

                List mostLikelyValues = this.forward.mostLikelyPath(megaState, this.lastMax.getValue(), getTime());

                System.out.println("MOST LIKELY PATH " + getTime() + " : " + mostLikelyValues);
            }

        }
    }

    public void extend() {

        this.time++;

        this.extend(this.transitionModels, this.time - 1, false);

        this.extend(this.captorsModels, this.time, true);
    }

    private void extend(Map<Variable, List<Model>> models, int timeParent, boolean captors) {

        List<Variable> newVars = new LinkedList<>();

        //extension du reseau pour chaque model d'extension
        for (Variable variable : models.keySet()) {

            //récupere la liste des modeles d'extension pour une variable
            //en fonction du temps utile pour les modele de markov d'ordre superieur à 1
            List<Model> varModels = models.get(variable);

            Model model;

            //si le temps est inferieur ou egal au nombre de modele de la variable
            if (this.time <= varModels.size()) {
                //récupere le modele correspondant au temps courant
                //un modele d'ordre de markov d'ordre trois serait limité à deux au temps 2 par exemple
                //dependences et TCP differente
                model = varModels.get(this.time - 1);

            } else {
                //sinon recupere le dernier modele celui le plus complet,
                //le nombre de dependences étant suffisantes
                model = varModels.get(varModels.size() - 1);
            }

            ArrayList<Variable> newDependencies = new ArrayList<>();

            //l'ordre des dependances et d'abord celui defini par le model
            //puis pour chacune des variables celles lié à un temps inférieur sont placés avant
            //en correspondance avec les entrées TCP
            for (Dependency dependencie : model.getDependencies()) {

                Variable lastDep = this.getTimeVariables(timeParent).get(dependencie.getDependency());

                // System.out.println("       DEP : "+dependencie.getDependency()+" "+dependencie.getMarkovOrder());

                //dependances à la même variable mais à des temps plus eloignés
                LinkedList<Variable> timeDependencies = new LinkedList<>();

                timeDependencies.add(lastDep);
                //récuperer les variables precedentes parmis les parents de la derniere dependance
                //si l'ordre est de 1 il ne se passe rien si supérieur à 1 on recupere
                //les mêmes variable de temps inférieur jusqu'à la limite d'ordre
                //(1)
                this.addDeeperDependencies(lastDep, timeDependencies, dependencie.getMarkovOrder() - 1);

                newDependencies.addAll(timeDependencies);
            }

            Variable newVar = new Variable(variable.getLabel(), variable.getDomain(), model.getProbabilityCompute(),
                    newDependencies, this.time);

            if (captors) {
                //pour les variables d'observation enregistre dans les variables parents leur indice
                //dans la liste des enfants
                newVar.saveObservation();
            }

            newVars.add(newVar);
        }

        //enregistrement des variables pour access rapide
        for (Variable newVar : newVars) {

            this.getTimeVariables(time).put(newVar, newVar);
        }

        //(1)
        //cependant peut poser problème en cas d'ancetre insuffisant
        //par exemple une variable qui aurait un modele de transition d'ordre 2
        //lors du deploiement de la variable t1 il ne pourrait récuperer q'un seul parent
        //en t0 par consequent il faudrait une TCP pour ces cas particulier
        //la solution est d'avoir des modeles de transition differents
        //avec des ordres différents et des TCP differentes pour les differents temps
        //en tout cas au début jusqu'à atteindre un temps qu permette d'avoir les
        //dependances completes
    }

    /*---------------------------GETTER SETTER -----------------------*/

    public int getTime() {

        return time;
    }

    public int getInitTime() {

        return initTime;
    }

    /*--------------------------- VIEW -----------------------*/

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append("------------------------------------------------------------------\n");
        builder.append("------------------------------NETWORK-----------------------------\n");
        builder.append("------------------------------------------------------------------\n\n");

        loadTree(this.roots, builder, 0);

        builder.append("\n\n");

        return builder.toString();
    }


    private void loadTree(List<Variable> vars, StringBuilder builder, int depth) {

        String ident = getTreeIdent(depth);

        for (Variable var : vars) {

            builder.append(ident);

            builder.append(var + "\n");

            loadTree(var.getChildren(), builder, depth + 1);
        }
    }

    public Map.Entry<Integer, Matrix> getLastForward() {
        return lastForward;
    }

    public void setLastForward(Matrix forward) {

        this.lastForward = new AbstractMap.SimpleEntry<>(this.getTime(), forward);
    }

    public Map.Entry<Integer, Matrix> getLastMax() {
        return lastMax;
    }

    public void setLastMax(Matrix max) {

        this.lastMax = new AbstractMap.SimpleEntry<>(this.getTime(), max);
    }

    public Forward getForward() {
        return forward;
    }

    public void setForward(Forward forward) {
        this.forward = forward;
    }
}
