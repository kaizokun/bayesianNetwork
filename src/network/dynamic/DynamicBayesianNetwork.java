package network.dynamic;

import domain.IDomain;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.Model.Dependency;

import java.util.*;

public class DynamicBayesianNetwork extends BayesianNetwork {

    protected int time = 0;

    protected Map<Integer, Map<Variable, Variable>> timeVariables = new Hashtable<>();

    protected Map<Variable, List<Model>> transitionModels = new Hashtable<>();

    protected Map<Variable, List<Model>> captorsModels = new Hashtable<>();

    public DynamicBayesianNetwork(AbstractDoubleFactory doubleFactory) {

        super(doubleFactory);
    }

    public Variable addRootVariable(String label, IDomain domain, ProbabilityCompute probabilityCompute) {

        Variable root = super.addRootVariable(label, domain, probabilityCompute);

        this.getTimeVariable(this.time).put(root, root);

        return root;
    }

    private Map<Variable, Variable> getTimeVariable(int time){

        Map<Variable, Variable> variables = this.timeVariables.get(time);

        if(variables == null && time <= this.time){

            variables = new Hashtable<>();

            this.timeVariables.put(time, variables);
        }

        return variables;
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

    private void addModel(Variable variable, Model model, Map<Variable,List<Model>> models){

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
    /*
    public void addTransitionModel(Variable variable, Model model, int time, int maxModels) {
        //recupere la liste des modeles par limite de temps atteinte pour une variable
        List<Model> varModels = this.transitionModels.get(variable);

        //crée et ajoute la liste si elle n'existe pas
        if (varModels == null) {

            varModels = new ArrayList<>(Arrays.asList(new Model[maxModels]));

            this.transitionModels.put(variable, varModels);
        }

        varModels.set(time, model);
    }

    public void addCaptorModel(Variable variable, Model model, int time, int maxModels) {
        //recupere la liste des modeles par limite de temps atteinte pour une variable
        List<Model> varModels = this.captorsModels.get(variable);

        //crée et ajoute la liste si elle n'existe pas
        if (varModels == null) {

            varModels = new ArrayList<>(Arrays.asList(new Model[maxModels]));

            this.transitionModels.put(variable, varModels);
        }

        varModels.set(time, model);
    }
*/

    private void addDeeperDependencies(Variable lastDep, LinkedList<Variable> timeDependencies, int limit) {

        if (limit > 0) {

            Variable depperDep = lastDep.getParent(lastDep.getTime() - 1);
            //ajoute les dependances plus lointaine dans le temps en premier
            timeDependencies.addFirst(depperDep);

            addDeeperDependencies(depperDep, timeDependencies, limit - 1);
        }
    }

    public void extend() {

        this.time ++;

        this.extend(this.transitionModels, this.time - 1);

        this.extend(this.captorsModels, this.time);
    }

    private void extend(Map<Variable, List<Model>> models, int timeParent){

        List<Variable> newVars = new LinkedList<>();

        //extension du reseau pour chaque model d'extension
        for (Variable variable : models.keySet()) {

            //récupere la liste des modeles d'extension pour une variable
            //en fonction du temps
            List<Model> varModels = models.get(variable);

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

            //l'ordre des dependances et d'abord celui defini par le model
            //puis pour chacune des variables celles lié à un temps inférieur sont placés avant
            //en correspondance avec les entrées TCP
            for (Dependency dependencie : model.getDependencies()) {

                Variable lastDep = this.getTimeVariable(timeParent).get(dependencie.getDependency());

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

            newVars.add(newVar);
        }

        //enregistrement des variables pour access rapide
        for (Variable newVar : newVars) {

            this.getTimeVariable(time).put(newVar, newVar);
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

    public void filter(List<Variable> request, List<Variable> observation){

        //on etend le graphe
        this.extend();

        /*
        *
        * premierement initialiser les observations à l'instant t à partir de valeur de la liste recu en parametre
        * idem pour la requete en sauvegardant les valeur precedentes
        *
        * Recuperer toutes les combinaison de valeur pour les variable de la requete via une méthode recursive
        *
        * total pour toutes les combinaisons
        *
        * Pour chaque combinaisons
        *
        *   initialiser les variables de la requete
        *
        *   totalCapteur <- 1.0
        *
        *   Pour chaque observation
        *
        *       totalCapteur = totalCapteur * Cacluler la probabilité de l'observation
        *                      en fonction de le valeur des variables d'états parent PO (normalement une etat par capteur)
        *
        *       SumHiddenState <- 0;
        *
        *       Pour chaque valeur pour la variable parent de PO PO2
        *           //cependant PO pourrait avoir plusieurs parents et il faudrait sommer sur chaque combinaison
        *           //de valeur pour les parents de PO
        *
        *           PO_value <- calculer la valeur de PO en fonction de PO2
        *
        *           probPO2 <- appel recursif de la fonction avec comme requete les variables PO2 et les observations liés à PO2
        *
        *           SumHiddenState <- SumHiddenState + ( PO_value * probPO2 )
        *
        *       Fin Pour
        *
        *   Fin Pour

        * Fin Pour
        *
        *
        *
        *
        *
        * */

    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        loadTree(this.roots, builder, 0);

        return builder.toString();
    }

    private void loadTree(List<Variable> vars, StringBuilder builder, int depth) {

        String ident = getIdent(depth);

        for(Variable var : vars){

            builder.append(ident);

            builder.append(var.getLabel()+" "+var.getTime()+"\n");

            loadTree(var.getChildren(), builder, depth + 1);
        }
    }

    private String getIdent(int depth){

        StringBuilder ident = new StringBuilder();

        for( int i = 0 ; i < depth - 1 ; i ++ ){

            ident.append("             ");
        }

        if(depth > 0) {

            ident.append("[____________");
        }

        return ident.toString();
    }

}
