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

public class DynamicBayesianNetwork extends BayesianNetwork {

    protected static String TOTAL_VALUE = "total";

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

    private Map<Variable, Variable> getTimeVariable(int time) {

        Map<Variable, Variable> variables = this.timeVariables.get(time);

        if (variables == null && time <= this.time) {

            variables = new Hashtable<>();

            this.timeVariables.put(time, variables);
        }

        return variables;
    }

    public Variable getVariable(int time, Variable variable){

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

    public  Map<Domain.DomainValue, AbstractDouble> filter(List<Variable> requests) {

        return this.filter(requests, 0);
    }

    private Map<java.lang.String, Map<Domain.DomainValue, AbstractDouble>> forwardDistribSaved = new Hashtable<>();

    private  Map<Domain.DomainValue, AbstractDouble> filter(List<Variable> requests, int depth) {

        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();

        List<Variable> requestObservations = new LinkedList<>();

        for (Variable req : requests) {

            req.saveOriginValue();

            requestObservations.addAll(req.getObservations());
        }

        if (requestObservations.isEmpty()) {

           // System.out.println();
           // System.out.println(getIdent(depth)+"VALUE : "+requests+" = "+requests.get(0).getProbabilityForCurrentValue());

            //en principe une seule variable
            //dan sl'appel recursif chaque variable caché et calculé séparemment en fonction des observations liées

            System.out.print(" "+requests.get(0).getProbabilityForCurrentValue()+" ");

            AbstractDouble total = doubleFactory.getNew(0.0);

            for(Domain.DomainValue domainValue : requests.get(0).getDomainValues()){

                requests.get(0).setDomainValue(domainValue);

                AbstractDouble valueProb = requests.get(0).getProbabilityForCurrentValue();

                distribution.put(domainValue, valueProb);

                total = total.add(valueProb);
            }

            distribution.put(new Domain.DomainValue(TOTAL_VALUE), total);

           // System.out.println();
           // System.out.println(requests+" "+distribution);
          //  System.out.println();
            return distribution;
        }

        List<List<Domain.DomainValue>> requestValuesList = BayesianNetwork.requestValuesCombinaisons(requests);

        AbstractDouble totalCombinaisons = this.doubleFactory.getNew(0.0);

        //AbstractDouble probRequest = this.doubleFactory.getNew(0.0);

        for (List<Domain.DomainValue> requestValues : requestValuesList) {

            int i = 0;

            for (Variable req : requests) {

                req.setDomainValue(requestValues.get(i ++));
            }

           // System.out.println();
           // System.out.println(getIdent(depth)+"COMBINAISON : "+requests);

            AbstractDouble probRequestCombinaison = this.doubleFactory.getNew(1.0);

            for(Variable observation : requestObservations){

                System.out.print(" {C} "+probRequestCombinaison+" * "+observation.getProbabilityForCurrentValue());

                probRequestCombinaison = probRequestCombinaison.multiply( observation.getProbabilityForCurrentValue() );

                Variable obsParentState = observation.getDependencies().get(0);

                System.out.print(" * SUM [ ");

                AbstractDouble obsParentStateValuesSum = this.doubleFactory.getNew(0.0);

                //on somme ensuite sur les dependances de la dependence de l'observation
                //une variable etat parent d'une observation peut avoir plusieurs autres états comme parents
                //une observation pourrait également avoir plusieurs états comme parent
                //dans ce cas peut être multiplier les resultats de chacune des sommations
                //pour l'instant on s'en tiens à un seul etat parent par observation

                List<List<Domain.DomainValue>> obsParentStateDependenciesValues = BayesianNetwork.requestValuesCombinaisons(obsParentState.getDependencies());

                int b = 1;

                for(List<Domain.DomainValue> domainValues : obsParentStateDependenciesValues){

                    int j = 0;

                    for(Variable obsParentStateDep : obsParentState.getDependencies()){

                        obsParentStateDep.setDomainValue(domainValues.get(j ++));
                    }

                    AbstractDouble mulTransitionForward = doubleFactory.getNew(1.0);

                    mulTransitionForward = mulTransitionForward.multiply(obsParentState.getProbabilityForCurrentValue());

                    System.out.print(" {T} "+obsParentState.getProbabilityForCurrentValue()+" * ( ");

                    int a = 1;

                    for(Variable obsParentStateDep : obsParentState.getDependencies()){

                        List<Variable> newRequests = new LinkedList<>();

                        newRequests.add(obsParentStateDep);

                        AbstractDouble forward;

                        String varKey = obsParentStateDep.getLabel()+"_"+obsParentStateDep.getTime();

                        if(forwardDistribSaved.containsKey(varKey)){

                            Map<Domain.DomainValue, AbstractDouble> distrib = forwardDistribSaved.get(varKey);

                            forward = distrib.get(obsParentStateDep.getDomainValue()).divide(distrib.get(new Domain.DomainValue(TOTAL_VALUE)));

                            System.out.print(forward);

                        }else {

                            Map<Domain.DomainValue, AbstractDouble> distrib = filter(newRequests, depth + 1);

                            forwardDistribSaved.put(varKey, distrib);

                            forward = distrib.get(obsParentStateDep.getDomainValue()).divide(distrib.get(new Domain.DomainValue(TOTAL_VALUE)));
                        }

                        if(a < obsParentState.getDependencies().size()){

                            System.out.print(" * ");
                        }

                        obsParentStateValuesSum = obsParentStateValuesSum.add( mulTransitionForward.multiply(forward) );

                        a++;
                    }

                    System.out.print(" ) ");

                    if(b < obsParentStateDependenciesValues.size()) {

                        System.out.print(" + ");
                    }

                    b ++;
                }

                System.out.print(" ]");

                probRequestCombinaison = probRequestCombinaison.multiply( obsParentStateValuesSum);
            }

            if(requests.size() == 1){

                distribution.put(requests.get(0).getDomainValue(), probRequestCombinaison);

            }else if(requests.size() > 1){

                distribution.put(new Domain.DomainValue(requestValues), probRequestCombinaison);
            }

            totalCombinaisons = totalCombinaisons.add(probRequestCombinaison);
/*
            boolean match = true;

            for(Variable request : requests){

                if(!request.originalValueMatch()){

                    match = false;

                    break;
                }
            }

            if(match){

                probRequest = probRequestCombinaison;
            }
*/
           // System.out.println();
          //  System.out.println(getIdent(depth)+"VALUE : "+requests+" = "+probRequestCombinaison);

        }

        distribution.put(new Domain.DomainValue(TOTAL_VALUE), totalCombinaisons);

        for (Variable req : requests) {

            req.setDomainValue(req.getOriginValue());
        }

      //  System.out.println();
      //  System.out.println(requests+" : "+distribution);
      //  System.out.println();
        return distribution;

        /*
        *
        *
        * */

        /*
        *
        * Recuperer les observations des variables de la requete
        *
        * Si aucune observation
        *
        *   retourner directement la probabilité de la variable
        *
        * Fin Si
        *
        *
        * le filtrage est appelée après avoir fourni les observations en t au reseaux
        * la requete se fait sur des variables pour lesquelles des observatiosn sont disponible en t
        *
        * Recuperer toutes les combinaison de valeur pour les variable de la requete
        *
        *
        * total pour toutes les combinaisons
        *
        * Pour chaque combinaison
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
        *       Recuperer les combinaison de valeurs parents pour les parents de PO
        *
        *       Pour chaque combinaison de valeur pour les variables parent de PO POPs
        *
        *           initialiser les valeurs des variables de POPs
        *
        *           PO_value <- calculer la valeur de PO en fonction de POPs
        *
        *           //on passe à l'appel récursif on il faudrait multiplier les valeurs retournées par la fonciton
        *           //pour chaque parent de PO en fonction des observations qui lui sont liés

        *           Pour chaque variable parent de POPs
        *
        *               PO_value <- PO_value * appel recursif de la fonction avec comme requete une variable PO2
        *
        *           Fin Pour
        *
        *           SumHiddenState <- SumHiddenState + PO_value

        *       Fin Pour
        *
        *       totalCapteur = totalCapteur * SumHiddenState;
        *
        *   Fin Pour

            enregistrer totalCapteur pour la combinaison de variables

        * Fin Pour
        *
        *
        * diviser la valeur obtenu pour la combinaison de valeur de la requete d'origine par le total de toutes les combinaisons
        *
        * pour obtenir la probabilité de la requete
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

        for (Variable var : vars) {

            builder.append(ident);

            builder.append(var+ "\n");

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
