package network.dynamic;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.Model.Dependency;

import java.util.*;

import static network.BayesianNetwork.requestValuesCombinations;

public class DynamicBayesianNetwork extends BayesianNetwork {

    protected static int maxDepth = Integer.MIN_VALUE;

    protected static String TOTAL_VALUE = "total";

    protected static Domain.DomainValue totalDomainValues = new Domain.DomainValue(TOTAL_VALUE);

    protected int time = 0;

    protected Map<Integer, Map<Variable, Variable>> timeVariables = new Hashtable<>();

    protected Map<Variable, List<Model>> transitionModels = new Hashtable<>();

    protected Map<Variable, List<Model>> captorsModels = new Hashtable<>();

    private Map<String, Map<Domain.DomainValue, AbstractDouble>> forwardDistribSaved = new Hashtable<>();

    public DynamicBayesianNetwork(AbstractDoubleFactory doubleFactory) {

        super(doubleFactory);
    }

    public Variable addRootVariable(String label, IDomain domain, ProbabilityCompute probabilityCompute) {

        Variable root = super.addRootVariable(label, domain, probabilityCompute);

        this.getTimeVariable(this.time).put(root, root);

        return root;
    }

    private Map<Variable, Variable> getTimeVariable(int time) {

        Map<Variable, Variable> variables = this.timeVariables.get(time);

        if (variables == null && time <= this.time) {

            variables = new Hashtable<>();

            this.timeVariables.put(time, variables);
        }

        return variables;
    }

    public Variable getVariable(int time, Variable variable) {

        return this.timeVariables.get(time).get(variable);
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

        this.time++;

        this.extend(this.transitionModels, this.time - 1, false);

        this.extend(this.captorsModels, this.time, true);
    }

    private void extend(Map<Variable, List<Model>> models, int timeParent, boolean captors) {

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

            if (captors) {
                //pour les variables d'observation enregistre dans les variables parents leur indice
                //dans la liste des enfants
                newVar.saveObservation();
            }

            newVars.add(newVar);
        }

        //enregistrement des variables pour access rapide
        for (Variable newVar : newVars) {

            this.getTimeVariable(time).put(newVar, newVar);
        }

        //(1)
        //cependant peut poser problème en    cas d'ancetre insuffisant
        //par exemple une variable qui aurait un modele de transition d'ordre 2 par exemple
        //lors du deploiement de la variable t1 il ne pourrait récuperer q'un seul parent
        //en t0 par consequent il faudrait une TCP pour ces cas particulier
        //la solution est d'avoir des modeles de transition differents
        //avec des ordres différents et des TCP differentes pour les differents temps
        //en tout cas au début jusqu'à atteindre un temps qu permette d'avoir les
        //dependances completes
    }

    public AbstractDouble predict(List<Variable> requests, int time){

        //étend le reseau jusqu'au temps voulu
        while(this.time < time){

            this.extend();
        }

        List<Variable> requests2 = new LinkedList<>();
        //pour chacune des variabel de la requete
        for(Variable req : requests){
            //récuperer la variable enregitré dans le reseau pour le temps time
            Variable networkVar = this.getVariable(time, req);
            //assigner la même valeur que demandé
            networkVar.setDomainValue(req.getDomainValue());

            requests2.add(networkVar);
        }

       return this.filter(requests2);
    }

    public AbstractDouble predict(Variable request, int time){

        //moyen le plus simple de gerer la prediction, il faut juste que le reseau soit etendu
        //juqu'au temps de prediction

        //étend le reseau jusqu'au temps voulu
        while(this.time < time){

            this.extend();
        }

        Variable request2 = this.getVariable(time, request);

        request2.setDomainValue(request.getDomainValue());

        return this.filter(request2);
    }

    public AbstractDouble filter(List<Variable> requests) {

        List<Domain.DomainValue> domainValues = new LinkedList<>();

        for (Variable request : requests) {

            domainValues.add(request.getDomainValue());
        }

        Map<Domain.DomainValue, AbstractDouble> distrib = this.filter(requests, 0);

        System.out.println("MAX DEPTH : "+maxDepth);

        return distrib.get(new Domain.DomainValue(domainValues)).divide(distrib.get(totalDomainValues));
    }

    public Map<Domain.DomainValue, AbstractDouble> filter(List<Variable> requests, int depth) {

        maxDepth = Math.max(maxDepth, depth);

        //enregistre les distributions pour chaque variable
        List<Map<Domain.DomainValue, AbstractDouble>> distributions = new ArrayList<>(requests.size());
        //appel independant de la methode de filtrage pour chaque variable de la requete
        for (Variable request : requests) {

            distributions.add(this.filter(request, depth));
        }

        //Il faut récuperer chaque combinaison de valeur pour chaque variable de la requete
        List<List<Domain.DomainValue>> requestValuesList = requestValuesCombinations(requests);

        Map<Domain.DomainValue, AbstractDouble> finalDistribution = new Hashtable<>();

        AbstractDouble total = doubleFactory.getNew(0.0);

        //pour chaque combinaison de valeurs
        for (List<Domain.DomainValue> domainValues : requestValuesList) {

            AbstractDouble totalCombination = doubleFactory.getNew(1.0);

            int iVar = 0;
            //pour chaque valeur de variable dans la combinaison
            for (Domain.DomainValue domainValue : domainValues) {

                //on récupère la probabilité correspondant à la valeur dans la distribution de la variable
                AbstractDouble varValueProb = distributions.get(iVar).get(domainValue);
                //que l'on multiplie avec celle de la variable de requete precedente
                totalCombination = totalCombination.multiply(varValueProb);

                /*
                AbstractDouble varValueProbNormalised = varValueProb.divide(distributions.get(iVar).get(new Domain.DomainValue(TOTAL_VALUE)));

                totalCombination = totalCombination.multiply(varValueProbNormalised);
                */
                iVar++;
            }

            finalDistribution.put(new Domain.DomainValue(domainValues), totalCombination);

            total = total.add(totalCombination);
        }

        finalDistribution.put(new Domain.DomainValue(TOTAL_VALUE), total);

        return finalDistribution;
    }

    public AbstractDouble filter(Variable request) {

        Domain.DomainValue requestDomainValue = request.getDomainValue();

        Map<Domain.DomainValue, AbstractDouble> distrib = this.filter(request, 0);

        System.out.println("MAX DEPTH : "+maxDepth);

        return distrib.get(requestDomainValue).divide(distrib.get(totalDomainValues));
    }

    private Map<Domain.DomainValue, AbstractDouble> filter(Variable request, int depth) {

        maxDepth = Math.max(maxDepth, depth);

        //les variables de requete d'origine doivent avoir le même temps

        //création d'une distribution vide pour chaque valeur de la requete
        //qui peuvent être des combinaisons de valeur si la reqete à plusieurs variables
        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();

        Domain.DomainValue originalValue = request.getDomainValue();

        //lors du rappel recursif de la méthode de filtrage une seule variable compose la requete
        //car même si une variable de la requete à plusieurs parents, dans la sommation sur les valeurs cachés
        //( çàd les parents de la requete ) ils sont evalués séparemment par rapport à leur observations respectifs
        //si une variable de requete unique et au temps zero on enregistre sa distribution et le total
        //pour ne pas avoir à le recalculer
        if (request.getTime() == 0) {

            //initialisation du total
            AbstractDouble total = doubleFactory.getNew(0.0);
            //pour chaque valeur du domaine de la requete
            for (Domain.DomainValue domainValue : request.getDomainValues()) {

                request.setDomainValue(domainValue);

                AbstractDouble valueProb = request.getProbabilityForCurrentValue();
                //enregistre la probabilité pour cette valeur
                distribution.put(domainValue, valueProb);

                total = total.add(valueProb);
            }
            //ajoute une entrée pour le total afin de normaliser
            distribution.put(new Domain.DomainValue(TOTAL_VALUE), total);

            request.setDomainValue(originalValue);

            return distribution;
        }
        //liste des observations à traiter pour l' ensemble de la requete
        List<Variable> requestObservations = new LinkedList<>(request.getObservations());

        //total pour toutes les valeurs de la requete
        AbstractDouble totalDistribution = this.doubleFactory.getNew(0.0);

        //pour chaque combinaison
        for (Domain.DomainValue domainValue : request.getDomainValues()) {

            request.setDomainValue(domainValue);

            //initialisation de la multiplication à 1
            AbstractDouble requestValueProbability = this.doubleFactory.getNew(1.0);

            //Pour chaque observations : ici on peut avoir une ou plusieurs observation par variable de requete
            for (Variable observation : requestObservations) {

                //au cas ou la variale d'observation est nul on obtient une prédiction plutot qu'un filtrage
                if(observation.getDomainValue() != null){
                    //valeur du modele de capteur
                    requestValueProbability = requestValueProbability.multiply(observation.getProbabilityForCurrentValue());
                }

                //pour chaque parent de l'observation on multiplie les sommations
                for (Variable stateObserved : observation.getDependencies()) {
                    //total de la somme sur les valeurs cachées initialisé à zero
                    AbstractDouble hiddenVarsSum = doubleFactory.getNew(1.0);

                    //sommation sur une seul variable caché
                    if (stateObserved.getDependencies().size() == 1) {

                        hiddenVarsSum = hiddenVarSum(stateObserved, stateObserved.getDependencies().get(0), depth);

                        //sommation sur plusieurs variables cachées
                    } else if (stateObserved.getDependencies().size() > 1) {

                        hiddenVarsSum = hiddenVarSum(stateObserved, stateObserved.getDependencies(), depth);
                    }

                    //qu'on multiplie avec les resultat pour les observations precedentes
                    requestValueProbability = requestValueProbability.multiply(hiddenVarsSum);
                }
            }

            //enregistrement de la probabilité pour la valeur cournate de la requete
            distribution.put(request.getDomainValue(), requestValueProbability);

            //addition du resultat pour une combinaison au total
            totalDistribution = totalDistribution.add(requestValueProbability);
        }
        //enregistre le total de la distribution
        distribution.put(new Domain.DomainValue(TOTAL_VALUE), totalDistribution);

        //valeur de la requete
        request.setDomainValue(originalValue);

        return distribution;
    }

    private AbstractDouble hiddenVarSum(Variable obsParentState, List<Variable> hiddenVars, int depth) {

        AbstractDouble hiddenVarsSum = this.doubleFactory.getNew(0.0);
        //une variable de la requete peut avoir plusieurs parents ils faut donc récuperer les combinaisons de valeurs
        //pour sommer sur chacune d'entre elle
        List<List<Domain.DomainValue>> hiddenVarsCombinations = requestValuesCombinations(hiddenVars);

        StringBuilder keybuilder = new StringBuilder();

        for (Variable hiddenVar : hiddenVars) {

            keybuilder.append(hiddenVar.getLabel() + "_" + hiddenVar.getTime());

            keybuilder.append(".");
        }

        //pour chaque combinaison de valeurs
        for (List<Domain.DomainValue> domainValues : hiddenVarsCombinations) {

            int j = 0;
            //initialise chaque variable caché avec une valeur de la combinaison
            for (Variable hiddenVar : hiddenVars) {

                hiddenVar.setDomainValue(domainValues.get(j++));
            }
            //début de la multiplication avec la valeur fourni par le modele de transition
            AbstractDouble mulTransitionForward = obsParentState.getProbabilityForCurrentValue();
            //on a ici potentiellement une filtrage sur plusieurs variable si plusieurs variables cachées

            Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(keybuilder.toString());

            if (distrib == null) {

                distrib = filter(hiddenVars, depth + 1);

                this.forwardDistribSaved.put(keybuilder.toString(), distrib);
            }

            AbstractDouble forward = distrib.get(new Domain.DomainValue(domainValues)).divide(distrib.get(new Domain.DomainValue(TOTAL_VALUE)));
            //on multiplie le resultat du filtre pour chaque variable cachée
            mulTransitionForward = mulTransitionForward.multiply(forward);

            //on additionne pour chaque combinaison de valeur pour les variables cachées
            hiddenVarsSum = hiddenVarsSum.add(mulTransitionForward);
        }

        return hiddenVarsSum;
    }

    private AbstractDouble hiddenVarSum(Variable obsParentState, Variable hiddenVar, int depth) {

        //méthode identique pour une seule variable cachée

        AbstractDouble hiddenVarsSum = this.doubleFactory.getNew(0.0);

        String key = hiddenVar.getLabel() + "_" + hiddenVar.getTime();

        for (Domain.DomainValue domainValue : hiddenVar.getDomainValues()) {

            hiddenVar.setDomainValue(domainValue);

            //début de la multiplication avec la valeur fourni par le modele de transition
            AbstractDouble mulTransitionForward = obsParentState.getProbabilityForCurrentValue();
            //on a ici potentiellement une filtrage sur plusieurs variable si plusieurs variables cachées

            Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(key);

            if (distrib == null) {

                distrib = filter(hiddenVar, depth + 1);

                this.forwardDistribSaved.put(key, distrib);
            }

            AbstractDouble forward = distrib.get(domainValue).divide(distrib.get(new Domain.DomainValue(TOTAL_VALUE)));
            //on multiplie le resultat du filtre pour la variable cachée
            mulTransitionForward = mulTransitionForward.multiply(forward);

            //on additionne pour chaque valeur
            hiddenVarsSum = hiddenVarsSum.add(mulTransitionForward);
        }

        return hiddenVarsSum;
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        loadTree(this.roots, builder, 0);

        return builder.toString();
    }

    private void loadTree(List<Variable> vars, StringBuilder builder, int depth) {

        String ident = getIdent(depth);

        for (Variable var : vars) {

            builder.append(ident);

            builder.append(var + "\n");

            loadTree(var.getChildren(), builder, depth + 1);
        }
    }

    private String getIdent(int depth) {

        StringBuilder ident = new StringBuilder();

        for (int i = 0; i < depth - 1; i++) {

            ident.append("             ");
        }

        if (depth > 0) {

            ident.append("[____________");
        }

        return ident.toString();
    }

}
