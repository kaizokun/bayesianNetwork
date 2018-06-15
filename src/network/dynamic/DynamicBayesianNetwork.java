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

    protected static boolean backwardLog = false;

    protected static int maxDepth = Integer.MIN_VALUE;

    protected static String TOTAL_VALUE = "total";

    protected static Domain.DomainValue totalDomainValues = new Domain.DomainValue(TOTAL_VALUE);

    protected int time = 0;

    protected Map<Integer, Map<Variable, Variable>> timeVariables = new Hashtable<>();

    protected Map<Variable, List<Model>> transitionModels = new Hashtable<>();

    protected Map<Variable, List<Model>> captorsModels = new Hashtable<>();

    private Map<String, Map<Domain.DomainValue, AbstractDouble>> forwardDistribSaved = new Hashtable<>();

    private Map<String, Map<Domain.DomainValue, AbstractDouble>> backwardDistribSaved = new Hashtable<>();

    private Map<String, Map<Domain.DomainValue, AbstractDouble>> maxDistribSaved = new Hashtable<>();

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


    /*---------------------------FILTERING PREDICTION SMOOTHING -----------------------*/

    /*---------------------------UTILS-----------------------*/


    private Map<Domain.DomainValue, AbstractDouble> getCombinatedDistribution(List<Variable> requests,
                                                                              List<Map<Domain.DomainValue, AbstractDouble>> distributions) {

        //Il faut récuperer chaque combinaison de valeur pour chaque variable de la requete
        List<List<Domain.DomainValue>> requestValuesList = requestValuesCombinations(requests);

        Map<Domain.DomainValue, AbstractDouble> finalDistribution = new Hashtable<>();

        AbstractDouble total = doubleFactory.getNew(0.0);

        //pour chaque combinaison de valeurs
        for (List<Domain.DomainValue> domainValues : requestValuesList) {

            AbstractDouble totalCombination = doubleFactory.getNew(1.0);

            int iRequest = 0;
            //pour chaque valeur de variable dans la combinaison
            for (Domain.DomainValue domainValue : domainValues) {

                //on récupère la probabilité correspondant à la valeur dans la distribution de la variable
                AbstractDouble varValueProb = distributions.get(iRequest).get(domainValue);
                //que l'on multiplie avec celle de la variable de requete precedente
                totalCombination = totalCombination.multiply(varValueProb);

                /*
                AbstractDouble varValueProbNormalised = varValueProb.divide(distributions.get(iVar).get(new Domain.DomainValue(TOTAL_VALUE)));

                totalCombination = totalCombination.multiply(varValueProbNormalised);
                */
                iRequest++;
            }

            finalDistribution.put(new Domain.DomainValue(domainValues), totalCombination);

            total = total.add(totalCombination);
        }


        /*
        if(requests.size() == 1) {
            //cas particulier ou la requete ne contient qu'une variable
            //normalement il faudrait eviter d'appeller cette fonction dans ce cas
            finalDistribution.put(new Domain.DomainValue(TOTAL_VALUE), distributions.get(0).get(totalDomainValues));

        }else{
*/
        finalDistribution.put(new Domain.DomainValue(TOTAL_VALUE), total);

        //}

        return finalDistribution;
    }


    private Map<Domain.DomainValue, AbstractDouble> multiplyDistributions(Map<Domain.DomainValue, AbstractDouble>... distributions) {

        //distribution final issue des distributions d'origine
        //et dont les valerus ont été multipliées pour chaque entrées qui sont identiques dans les tables
        Map<Domain.DomainValue, AbstractDouble> finalDistribution = new Hashtable<>();
        //pour chaque clé de la table : un Object DomainValue ( qui peut être composé d'une liste de DomainValue
        //cas de plusieurs variable par requete )
        for (Domain.DomainValue domainValue : distributions[0].keySet()) {

            if (domainValue.equals(totalDomainValues)) continue;

            AbstractDouble valueTotal = doubleFactory.getNew(1.0);
            //multiplie les entrées pour chaque clé une par une dans chaque distribution
            for (Map<Domain.DomainValue, AbstractDouble> distribution : distributions) {

                valueTotal = valueTotal.multiply(distribution.get(domainValue));
            }

            finalDistribution.put(domainValue, valueTotal);
        }

        return finalDistribution;
    }

    private Map<Domain.DomainValue, AbstractDouble> normalizeDistribution(Map<Domain.DomainValue, AbstractDouble> distrib) {

        Map<Domain.DomainValue, AbstractDouble> distribNormalized = new Hashtable<>();

        for (Domain.DomainValue domainValue : distrib.keySet()) {

            distribNormalized.put(domainValue, distrib.get(domainValue).divide(distrib.get(totalDomainValues)));
        }

        return distribNormalized;
    }

    private void addTotalToDistribution(Map<Domain.DomainValue, AbstractDouble> distributionFinal) {

        AbstractDouble total = doubleFactory.getNew(0.0);

        for (AbstractDouble value : distributionFinal.values()) {

            total = total.add(value);
        }

        distributionFinal.put(totalDomainValues, total);
    }


    private String getDistribSavedKey(List<Variable> variables, int time) {

        StringBuilder keybuilder = new StringBuilder();

        for (Variable variable : variables) {

            keybuilder.append(variable.getLabel() + "_" + time);

            keybuilder.append('.');
        }

        keybuilder.deleteCharAt(keybuilder.length() - 1);

        return keybuilder.toString();
    }

    private String getDistribSavedKey(List<Variable> variables) {

        StringBuilder keybuilder = new StringBuilder();

        for (Variable variable : variables) {

            keybuilder.append(variable.getLabel() + "_" + variable.getTime());

            keybuilder.append('.');
        }

        keybuilder.deleteCharAt(keybuilder.length() - 1);

        return keybuilder.toString();
    }

    private String getDistribSavedKey(Variable variable) {

        return this.getDistribSavedKey(variable, variable.getTime());
    }

    private String getDistribSavedKey(Variable variable, int time) {

        return variable.getLabel() + "_" + time;
    }
    /*------------------- FORWARD BACKWARD --------------------*/

    /*
     * forwardBakward time
     *
     * faire le forward pour la ou les variable de requete au temps time
     *
     *
     *
     *
     * */

    public void forwardBackward(List<Variable> requests){

        this.forwardBackward(requests, 0, this.time, this.time);
    }

    public void forwardBackward(List<Variable> requests, int smootStart, int smootEnd) {

        this.forwardBackward(requests, smootStart, smootEnd, this.time);
    }

    public void forwardBackward(List<Variable> requests, int smootStart, int smootEnd, int backWardEnd) {

        List<Variable> startForwardVars = new LinkedList<>();

        //variables situées au temps T
        for (Variable req : requests) {

            startForwardVars.add(this.getVariable(smootEnd, req));
        }

        List<Variable> startBackwardVars = new LinkedList<>();

        //variables situées au temps 1
        for (Variable req : requests) {

            startBackwardVars.add(this.getVariable(smootStart, req));
        }

        String forwardKey = this.getDistribSavedKey(startForwardVars);

        String backwardKey = this.getDistribSavedKey(startBackwardVars);

        this.forward(startForwardVars, forwardKey, 0);

        this.backWard(startBackwardVars, backwardKey, 0, backWardEnd);

        Map<String, Map<Domain.DomainValue, AbstractDouble>> forwardDistributionsNormal = new Hashtable<>();

        Map<String, Map<Domain.DomainValue, AbstractDouble>> smootDistributions = new Hashtable<>();

        Map<String, Map<Domain.DomainValue, AbstractDouble>> smootDistributionsNormal = new Hashtable<>();

        this.showForward();

        this.showBackward();

        for (int time = smootStart; time <= smootEnd; time ++) {

            String keyForward = getDistribSavedKey(requests, time);

            String keyBackWard = getDistribSavedKey(requests, time);

            Map<Domain.DomainValue, AbstractDouble> smootDistrib = this.smootDistribution(
                    this.forwardDistribSaved.get(keyForward),
                    this.backwardDistribSaved.get(keyBackWard));

            smootDistributions.put(keyForward, smootDistrib);

            smootDistributionsNormal.put(keyForward, normalizeDistribution(smootDistrib));

            forwardDistributionsNormal.put(keyForward, normalizeDistribution(this.forwardDistribSaved.get(keyForward)));
        }

        this.showDistributions("FORWARD NORMAL", forwardDistributionsNormal);

        this.showDistributions("FORWARDxBACKWARD", smootDistributions);

        this.showDistributions("FORWARDxBACKWARD NORMAL", smootDistributionsNormal);
    }

    public void forwardBackward(Variable request) {

        this.forwardBackward(request, 0, this.time, this.time);
    }

    public void forwardBackward(Variable request, int smootStart, int smootEnd) {

        this.forwardBackward(request, smootStart, smootEnd, this.time);
    }

    public void forwardBackward(Variable request, int smootStart, int smootEnd, int backWardEnd) {

        System.out.println("[ "+smootStart+" - "+smootEnd+" ]  "+backWardEnd);

        /*
         * timeStart : debut de l'intervalle pour les variable à lisser
         * timeStart : fin de l'intervalle
         *
         * exemple : on souhaite lisser entre 5 et 8 avec un temps courant de 10
         *
         * il faut calculer le forward sur l'intervalle 0 à 5 le forward etant initialisé au temps 0
         * et calculer le backward sur l'intervalle 10 à 5 le backward etant initialisé au temps final
         * bien qu'il pourrait l'être depuis un temps précédent ici backWardEnd.
         *
         * */

        //on travaille sur la variable de la requete situé au temps smootEnd
        Variable startForwardVar = this.getVariable(smootEnd, request);
        //on travaille sur la variable de la requete situé au temps smootStart
        Variable startBackwardVar = this.getVariable(smootStart, request);

        String forwardKey = this.getDistribSavedKey(startForwardVar);

        String backwardKey = this.getDistribSavedKey(startBackwardVar);
        //forward sur les temps situé avant la borne supérieur de l'intervalle comprise
        this.forward(startForwardVar, forwardKey, 0);
        //backward sur les temps situé depuis la borne inférieur de l'intervalle jusqu'à la fin ou une limite choisi
        //supérieure à la borne de fin de l'intervalle
        this.backWard(startBackwardVar, backwardKey, 0, backWardEnd);

        Map<String, Map<Domain.DomainValue, AbstractDouble>> forwardDistributionsNormal = new Hashtable<>();

        Map<String, Map<Domain.DomainValue, AbstractDouble>> smootDistributions = new Hashtable<>();

        Map<String, Map<Domain.DomainValue, AbstractDouble>> smootDistributionsNormal = new Hashtable<>();

        this.showForward();

        this.showBackward();

        for (int time = smootStart; time <= smootEnd; time++) {

            String keyForward = getDistribSavedKey(request, time);

            String keyBackWard = getDistribSavedKey(request, time);

            Map<Domain.DomainValue, AbstractDouble> smootDistrib = this.smootDistribution(
                    this.forwardDistribSaved.get(keyForward),
                    this.backwardDistribSaved.get(keyBackWard));

            smootDistributions.put(keyForward, smootDistrib);

            smootDistributionsNormal.put(keyForward, normalizeDistribution(smootDistrib));

            forwardDistributionsNormal.put(keyForward, normalizeDistribution(this.forwardDistribSaved.get(keyForward)));
        }

        this.showDistributions("FORWARD NORMAL", forwardDistributionsNormal);

        this.showDistributions("FORWARDxBACKWARD", smootDistributions);

        this.showDistributions("FORWARDxBACKWARD NORMAL", smootDistributionsNormal);
    }

    public void showForward() {

        showDistributions("FORWARD", this.forwardDistribSaved);
    }

    public void showBackward() {

        showDistributions("BACKWARD", this.backwardDistribSaved);
    }

    private void showDistributions(String title, Map<String, Map<Domain.DomainValue, AbstractDouble>> distributions) {

        System.out.println();
        System.out.println(title + " " + distributions.size());
        System.out.println();

        for (Map.Entry<String, Map<Domain.DomainValue, AbstractDouble>> entry : distributions.entrySet()) {

            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }

    /*------------------- SMOOTHING--------------------*/

    private Map<Domain.DomainValue, AbstractDouble> smootDistribution(Map<Domain.DomainValue, AbstractDouble> forward,
                                                                      Map<Domain.DomainValue, AbstractDouble> backward) {

        //normalisation de la distribution forward
        Map<Domain.DomainValue, AbstractDouble> forwardNormal = this.normalizeDistribution(forward);
        //suppression des valeurs totales
        Map<Domain.DomainValue, AbstractDouble> smootDistrib = multiplyDistributions(forwardNormal, backward);

        this.addTotalToDistribution(smootDistrib);

        return smootDistrib;
    }

    public AbstractDouble smoothing(List<Variable> requests) {
        //lissage de la variable
        return this.smoothing(requests, this.time);
    }

    protected AbstractDouble smoothing(List<Variable> requests, int timeEnd) {

        List<Domain.DomainValue> domainValues = new LinkedList<>();

        String key = this.getDistribSavedKey(requests);

        for (Variable request : requests) {

            domainValues.add(request.getDomainValue());
        }

        this.forward(requests, key, 0);

        this.backWard(requests, key, 0, timeEnd);

        Map<Domain.DomainValue, AbstractDouble> distributionFinal =
                this.smootDistribution(this.forwardDistribSaved.get(key), this.backwardDistribSaved.get(key));

        return distributionFinal.get(new Domain.DomainValue(domainValues)).divide(distributionFinal.get(totalDomainValues));
    }

    public AbstractDouble smoothing(Variable request) {

        return this.smoothing(request, this.time);
    }

    public AbstractDouble smoothing(Variable request, int timeEnd) {

        String key = this.getDistribSavedKey(request);

        this.forward(request, key, 0);

        this.backWard(request, key, 0, timeEnd);

        Map<Domain.DomainValue, AbstractDouble> distributionFinal =
                smootDistribution(this.forwardDistribSaved.get(key), this.backwardDistribSaved.get(key));

        return distributionFinal.get(request.getDomainValue()).divide(distributionFinal.get(totalDomainValues));
    }



    /*------------------- BACKWARD --------------------*/

    /*
     *      * Backward sur plusieurs
     *      *
     *      * à l'origine si il y a plusieurs variables dans la requete
     *      * faire plusieurs appels de base à backward pour chaque variable et recuperer une distribution sur la variable
     *      * qu'il faudra combiner à la fin en multipliant les valeurs pour chaque combinaison de valeur de la requete
     *      * et recreer une distribution composée
     * */

    public void backWard(List<Variable> requests, String key, int depth, int timeEnd) {

        //enregistre les distributions pour chaque variable
        List<Map<Domain.DomainValue, AbstractDouble>> distributions = new ArrayList<>(requests.size());

        //appel independant de la methode backward pour chaque variable de la requete
        for (Variable request : requests) {

            String key2 = this.getDistribSavedKey(request);

            this.backWard(request, key2, depth, timeEnd);

            distributions.add(this.backwardDistribSaved.get(key2));
        }

        Map<Domain.DomainValue, AbstractDouble> finalDistrib = this.getCombinatedDistribution(requests, distributions);

        this.backwardDistribSaved.put(key, finalDistrib);
    }

    /*

     * Backward sur une seule variable
     *
     * récuperer toutes les observations de la variable état si tenté qu'il y en a plus d'une ...
     *
     * pour le temps suivant
     *
     * creer une distribution sur les valeurs de la requete
     *
     * sauvegarder la valeur de la requete
     *
     * initialiser un total TOTAL_REQUEST : le total de la distribution pour chaque valeur de la requete
     *
     * Pour chaque valeur de la variable de requete
     *
     *   initialiser la variable de requete
     *
     *   initialiser une MUL_OBS à 1.0 pour multiplier le resultat de chaque observations de la requete
     *
     *   Pour chaque observation
     *
     *       recuperer les combinaison de valeur des parents de l'observation
     *
     *       initialiser un total SUM pour la somme sur les parents de l'observation
     *
     *       Pour chaque combinaison de parent
     *
     *           initialiser une valeur MUL à 1.0 pour multiplier les parties de la somme
     *
     *           initialiser les variables parents de l'observation
     *
     *           MUL <- multiplier MUL par la probabilité de l'observation en fonction de la combinaison de valeurs parents courante
     *
     *           récuperer la distribution pour les parents de la variable d'observation dans une map
     *           ayant pour clé la combinaison des variables parents : label + temps de chaque variables concaténées
     *
     *           Si cette distribution n'existe pas encore
     *
     *               rappel recursif de la fonction backward avec les parents de la variable d'observation
     *
     *               recuperation et sauvegarde de la distribution
     *
     *           Fin Si
     *
     *           recuperer la probabilité pour la combinaison courante de valeurs parents
     *
     *           diviser cette probabilité par le total enregistré pour toutes les combinaisons
     *
     *           MUL <- multiplier MUL par cette valeur
     *
     *           Pour chaque parent de l'observation
     *
     *               MUL <- multiplier MUL par la probabilité d'un parent en fonction des siens
     *
     *           Fin Pour
     *
     *           SUM <- additionner MUL à SUM
     *
     *       Fin Pour
     *
     *       MUL_OBS <- multiplier MUL_OBS à SUM
     *
     *   Fin Pour
     *
     *   enregistrer MUL_OBS à la valeur de la requete (clé) dans la distribution
     *
     *   TOTAL_REQUEST <- TOTAL_REQUEST + MUL_OBS
     *
     * Fin Pour
     *
     *
     * enregistrer TOTAL_REQUEST à la valeur TOTAL dans la distribution
     *
     * retourner la distribution
     *
     * */

    public void backWard(Variable request, String key, int depth, int timeEnd) {

        //System.out.println(this.getIdent(depth)+" BACKWARD "+request);

        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();

        //atteind une requete située à la derniere phase temporelle
        if (request.getTime() == timeEnd) {

            for (Domain.DomainValue domainValue : request.getDomainValues()) {

                distribution.put(domainValue, doubleFactory.getNew(1.0));
            }

            distribution.put(totalDomainValues, doubleFactory.getNew(1.0));

            this.backwardDistribSaved.put(key, distribution);

            return;
        }
        //récuperation des observations de la variable au temps suivant
        List<Variable> nextObservations = this.getVariable(request.getTime() + 1, request).getObservations();

        Domain.DomainValue savedDomainValue = request.getDomainValue();

        AbstractDouble totalRequest = doubleFactory.getNew(0.0);

        //pour chaque valeru de la requete
        for (Domain.DomainValue requestValue : request.getDomainValues()) {

            request.setDomainValue(requestValue);
            //multiplier le resultat pour chaque observation
            AbstractDouble multiplyObservations = doubleFactory.getNew(1.0);

            for (Variable nextObservation : nextObservations) {

                if (nextObservation.getDependencies().size() == 1) {

                    multiplyObservations = multiplyObservations.multiply(
                            backwardSum(nextObservation.getDependencies().get(0), nextObservation, depth, timeEnd));

                } else if (nextObservation.getDependencies().size() > 1) {

                    multiplyObservations = multiplyObservations.multiply(
                            backwardSum(nextObservation.getDependencies(), nextObservation, depth, timeEnd));
                }
            }

            distribution.put(requestValue, multiplyObservations);

            totalRequest = totalRequest.add(multiplyObservations);
        }

        distribution.put(totalDomainValues, totalRequest);

        request.setDomainValue(savedDomainValue);

        this.backwardDistribSaved.put(key, distribution);
    }

    public AbstractDouble backwardSum(List<Variable> hiddenVars, Variable nextObservation, int depth, int timeEnd) {

        AbstractDouble sum = doubleFactory.getNew(0.0);

        List<List<Domain.DomainValue>> hiddenVarsValues =
                BayesianNetwork.requestValuesCombinations(hiddenVars);

        String key = getDistribSavedKey(hiddenVars);

        for (List<Domain.DomainValue> domainValues : hiddenVarsValues) {

            AbstractDouble multiplyUnderSum = doubleFactory.getNew(1.0);

            int d = 0;

            for (Domain.DomainValue depValue : domainValues) {

                nextObservation.getDependencies().get(d).setDomainValue(depValue);

                d++;
            }

            if (backwardLog) System.out.print(nextObservation.getProbabilityForCurrentValue());

            multiplyUnderSum = multiplyUnderSum.multiply(nextObservation.getProbabilityForCurrentValue());

            Map<Domain.DomainValue, AbstractDouble> hiddenVarsDistribution = this.backwardDistribSaved.get(key);

            if (hiddenVarsDistribution == null) {

                this.backWard(nextObservation.getDependencies(), key, depth + 1, timeEnd);

                hiddenVarsDistribution = this.backwardDistribSaved.get(key);
            }

            AbstractDouble combinaisonProb = hiddenVarsDistribution.get(new Domain.DomainValue(domainValues));

            if (backwardLog)
                System.out.print(" * " + combinaisonProb.divide(hiddenVarsDistribution.get(totalDomainValues)));

            multiplyUnderSum = multiplyUnderSum.multiply(combinaisonProb.divide(hiddenVarsDistribution.get(totalDomainValues)));

            for (Variable hiddenVar : nextObservation.getDependencies()) {

                if (backwardLog) System.out.print(" * " + hiddenVar.getProbabilityForCurrentValue());

                multiplyUnderSum = multiplyUnderSum.multiply(hiddenVar.getProbabilityForCurrentValue());
            }

            if (backwardLog) System.out.print(" + ");

            sum = sum.add(multiplyUnderSum);
        }

        return sum;
    }

    public AbstractDouble backwardSum(Variable hiddenVar, Variable nextObservation, int depth, int timeEnd) {

        AbstractDouble sum = doubleFactory.getNew(0.0);

        String key = getDistribSavedKey(hiddenVar);

        for (Domain.DomainValue domainValue : hiddenVar.getDomainValues()) {

            AbstractDouble multiplyUnderSum = doubleFactory.getNew(1.0);

            hiddenVar.setDomainValue(domainValue);

            if (backwardLog) System.out.print(nextObservation.getProbabilityForCurrentValue());

            multiplyUnderSum = multiplyUnderSum.multiply(nextObservation.getProbabilityForCurrentValue());

            Map<Domain.DomainValue, AbstractDouble> hiddenVarsDistribution = this.backwardDistribSaved.get(key);

            if (hiddenVarsDistribution == null) {

                this.backWard(hiddenVar, key, depth + 1, timeEnd);

                hiddenVarsDistribution = this.backwardDistribSaved.get(key);
            }

            AbstractDouble hiddenVarProb = hiddenVarsDistribution.get(domainValue);

            if (backwardLog)
                System.out.print(" * " + hiddenVarProb.divide(hiddenVarsDistribution.get(totalDomainValues)));

            multiplyUnderSum = multiplyUnderSum.multiply(hiddenVarProb.divide(hiddenVarsDistribution.get(totalDomainValues)));

            if (backwardLog) System.out.print(" * " + hiddenVar.getProbabilityForCurrentValue());

            multiplyUnderSum = multiplyUnderSum.multiply(hiddenVar.getProbabilityForCurrentValue());

            if (backwardLog) System.out.print(" + ");

            sum = sum.add(multiplyUnderSum);
        }

        return sum;
    }

    /*------------------- PREDICTION--------------------*/

    public AbstractDouble prediction(List<Variable> requests, int time) {

        //étend le reseau jusqu'au temps voulu
        while (this.time < time) {

            this.extend();
        }

        List<Variable> requests2 = new LinkedList<>();
        //pour chacune des variables de la requete
        for (Variable req : requests) {
            //récuperer la variable enregitré dans le reseau pour le temps time
            Variable networkVar = this.getVariable(time, req);
            //assigner la même valeur que demandé
            networkVar.setDomainValue(req.getDomainValue());

            requests2.add(networkVar);
        }

        return this.filtering(requests2);
    }

    public AbstractDouble prediction(Variable request, int time) {

        //moyen le plus simple de gerer la prediction, il faut juste que le reseau soit etendu
        //juqu'au temps de prediction

        //étend le reseau jusqu'au temps voulu
        while (this.time < time) {

            this.extend();
        }

        Variable request2 = this.getVariable(time, request);

        request2.setDomainValue(request.getDomainValue());

        return this.filtering(request2);
    }


    /*------------------- FILTERING--------------------*/


    public AbstractDouble filtering(List<Variable> requests) {

        String key = this.getDistribSavedKey(requests);

        List<Domain.DomainValue> domainValues = new LinkedList<>();

        for (Variable request : requests) {

            domainValues.add(request.getDomainValue());
        }

        this.forward(requests, key, 0);

        Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(key);

        System.out.println("MAX DEPTH : " + maxDepth);

        return distrib.get(new Domain.DomainValue(domainValues)).divide(distrib.get(totalDomainValues));
    }


    public AbstractDouble filtering(Variable request) {

        String key = this.getDistribSavedKey(request);

        Domain.DomainValue requestDomainValue = request.getDomainValue();

        this.forward(request, key, 0);

        Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(key);

        System.out.println("MAX DEPTH : " + maxDepth);

        return distrib.get(requestDomainValue).divide(distrib.get(totalDomainValues));
    }


    /*------------------- FORWARD--------------------*/

    public void forward(List<Variable> requests, String key, int depth) {

        maxDepth = Math.max(maxDepth, depth);

        //enregistre les distributions pour chaque variable
        List<Map<Domain.DomainValue, AbstractDouble>> distributions = new ArrayList<>(requests.size());
        //appel independant de la methode de filtrage pour chaque variable de la requete
        for (Variable request : requests) {

            String key2 = this.getDistribSavedKey(request);

            this.forward(request, key2, depth);

            distributions.add(this.forwardDistribSaved.get(key2));
        }

        this.forwardDistribSaved.put(key, getCombinatedDistribution(requests, distributions));
    }

    private void forward(Variable request, String key, int depth) {

        // System.out.println(this.getIdent(depth)+" FORWARD "+request);

        maxDepth = Math.max(maxDepth, depth);

        //les variables de requete d'origine doivent avoir le même temps

        //création d'une distribution vide pour chaque valeur de la requete
        //qui peuvent être des combinaisons de valeur si la reqete à plusieurs variables
        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();

        Map<Domain.DomainValue, AbstractDouble> maxDistribution = new Hashtable<>();

        Domain.DomainValue originalValue = request.getDomainValue();

        //total pour toutes les valeurs de la requete
        AbstractDouble totalDistribution = this.doubleFactory.getNew(0.0);

        //max pour toutes les valeurs de la requete
        AbstractDouble maxDomValue = this.doubleFactory.getNew(0.0);

        //lors du rappel recursif de la méthode de filtrage une seule variable compose la requete
        //car même si une variable de la requete à plusieurs parents, dans la sommation sur les valeurs cachés
        //( çàd les parents de la requete ) ils sont evalués séparemment par rapport à leur observations respectifs
        //si une variable de requete unique et au temps zero on enregistre sa distribution et le total
        //pour ne pas avoir à le recalculer
        if (request.getTime() == 0) {

            //pour chaque valeur du domaine de la requete
            for (Domain.DomainValue domainValue : request.getDomainValues()) {

                request.setDomainValue(domainValue);

                AbstractDouble valueProb = request.getProbabilityForCurrentValue();
                //enregistre la probabilité pour cette valeur
                distribution.put(domainValue, valueProb);

                totalDistribution = totalDistribution.add(valueProb);
                //compare la probabilité avec le maximum actuel
                int cmp = valueProb.compareTo(maxDomValue);
                //si supérieur au max
                if(cmp > 0){
                    //sauvegarde le nouveau max
                    maxDomValue = valueProb;
                    //supprimmer le(s) ancienne(s) valeur(s) de domaine liés au max précedents
                    maxDistribution.clear();
                    //lié la valeur max à la valeur de domaine
                    maxDistribution.put(domainValue, valueProb);

                }else if (cmp == 0){
                    //si égalité avec un max précédent on ajoute la valeur de domaine
                    maxDistribution.put(domainValue, valueProb);
                }
            }
            //ajoute une entrée pour le total afin de normaliser
            distribution.put(new Domain.DomainValue(TOTAL_VALUE), totalDistribution);

            request.setDomainValue(originalValue);

            this.forwardDistribSaved.put(key, distribution);

            this.maxDistribSaved.put(key, maxDistribution);

            return;
        }
        //liste des observations à traiter pour l' ensemble de la requete
        List<Variable> requestObservations = new LinkedList<>(request.getObservations());

        //pour chaque combinaison
        for (Domain.DomainValue domainValue : request.getDomainValues()){

            request.setDomainValue(domainValue);

            //initialisation de la multiplication à 1
            AbstractDouble requestValueProbability = this.doubleFactory.getNew(1.0);

            AbstractDouble requestValueMaxProbability = this.doubleFactory.getNew(1.0);

            //Pour chaque observations : ici on peut avoir une ou plusieurs observation par variable de requete
            for (Variable observation : requestObservations) {

                //au cas ou la variale d'observation est nul on obtient une prédiction plutot qu'un filtrage
                if (observation.getDomainValue() != null) {
                    //valeur du modele de capteur
                    requestValueProbability = requestValueProbability.multiply(observation.getProbabilityForCurrentValue());

                    requestValueMaxProbability = requestValueMaxProbability.multiply(observation.getProbabilityForCurrentValue());
                }

                //pour chaque parent de l'observation on multiplie les sommations
                for (Variable stateObserved : observation.getDependencies()) {
                    //total de la somme sur les valeurs cachées initialisé à zero

                    AbstractDouble hiddenVarsSum = doubleFactory.getNew(1.0),
                                   hiddenVarsMax = doubleFactory.getNew(1.0);

                    //sommation sur une seul variable caché
                    if (stateObserved.getDependencies().size() == 1) {

                        ForwardSumRs rs = forwardHiddenVarSum( stateObserved, stateObserved.getDependencies().get(0), depth);

                        hiddenVarsSum = rs.sum;

                        hiddenVarsMax = rs.max;

                        //sommation sur plusieurs variables cachées
                    } else if (stateObserved.getDependencies().size() > 1) {

                        ForwardSumRs rs = forwardHiddenVarSum( stateObserved, stateObserved.getDependencies(), depth);

                        hiddenVarsSum = rs.sum;

                        hiddenVarsMax = rs.max;
                    }

                    //qu'on multiplie avec les resultat pour les observations precedentes
                    requestValueProbability = requestValueProbability.multiply(hiddenVarsSum);

                    requestValueMaxProbability = requestValueMaxProbability.multiply(hiddenVarsMax);
                }
            }

            //enregistrement de la probabilité pour la valeur courante de la requete
            distribution.put(request.getDomainValue(), requestValueProbability);

            //addition du resultat pour une combinaison au total
            totalDistribution = totalDistribution.add(requestValueProbability);

            int cmp = requestValueMaxProbability.compareTo(maxDomValue);

            //si supérieur au max
            if(cmp > 0){
                //sauvegarde le nouveau max
                maxDomValue = requestValueMaxProbability;
                //supprimmer le(s) ancienne(s) valeur(s) de domaine liés au max précedents
                maxDistribution.clear();
                //lié la valeur max à la valeur de domaine
                maxDistribution.put(domainValue, requestValueMaxProbability);

            }else if (cmp == 0){
                //si égalité avec un max précédent on ajoute la valeur de domaine
                maxDistribution.put(domainValue, requestValueMaxProbability);
            }

        }
        //enregistre le total de la distribution
        distribution.put(totalDomainValues, totalDistribution);

        //valeur de la requete
        request.setDomainValue(originalValue);

        this.maxDistribSaved.put(key, maxDistribution);

        this.forwardDistribSaved.put(key, distribution);
    }

    private ForwardSumRs forwardHiddenVarSum(Variable obsParentState, List<Variable> hiddenVars, int depth) {

        AbstractDouble hiddenVarMax = this.doubleFactory.getNew(0.0);

        AbstractDouble hiddenVarsSum = this.doubleFactory.getNew(0.0);
        //une variable de la requete peut avoir plusieurs parents ils faut donc récuperer les combinaisons de valeurs
        //pour sommer sur chacune d'entre elle
        List<List<Domain.DomainValue>> hiddenVarsCombinations = requestValuesCombinations(hiddenVars);

        String key = getDistribSavedKey(hiddenVars);

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

            Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(key);

            Map<Domain.DomainValue, AbstractDouble> max = this.maxDistribSaved.get(key);

            if (distrib == null) {

                this.forward(hiddenVars, key, depth + 1);

                max = this.maxDistribSaved.get(key);

                distrib = this.forwardDistribSaved.get(key);
            }
            //si plusieurs valeur de domaine elles ont la même prob on recupere la prob du premier
            AbstractDouble maxValue = max.values().iterator().next();

            maxValue = maxValue.multiply(obsParentState.getProbabilityForCurrentValue());

            if(maxValue.compareTo(hiddenVarMax) > 0){

                hiddenVarMax = maxValue;
            }

            AbstractDouble forward = distrib.get(new Domain.DomainValue(domainValues)).divide(distrib.get(totalDomainValues));
            //on multiplie le resultat du filtre pour chaque variable cachée
            mulTransitionForward = mulTransitionForward.multiply(forward);

            //on additionne pour chaque combinaison de valeur pour les variables cachées
            hiddenVarsSum = hiddenVarsSum.add(mulTransitionForward);
        }

        return new ForwardSumRs(hiddenVarsSum, hiddenVarMax);
    }


    private ForwardSumRs forwardHiddenVarSum(Variable obsParentState, Variable hiddenVar, int depth) {

        //méthode identique pour une seule variable cachée

        AbstractDouble hiddenVarMax = this.doubleFactory.getNew(0.0);

        AbstractDouble hiddenVarSum = this.doubleFactory.getNew(0.0);

        String key = getDistribSavedKey(hiddenVar);

        for (Domain.DomainValue domainValue : hiddenVar.getDomainValues()) {

            hiddenVar.setDomainValue(domainValue);

            //début de la multiplication avec la valeur fourni par le modele de transition
            AbstractDouble mulTransitionForward = obsParentState.getProbabilityForCurrentValue();
            //on a ici potentiellement une filtrage sur plusieurs variable si plusieurs variables cachées

            Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(key);

            Map<Domain.DomainValue, AbstractDouble> max = this.maxDistribSaved.get(key);

            if (distrib == null) {

                this.forward(hiddenVar, key, depth + 1);

                distrib = this.forwardDistribSaved.get(key);

                max = this.maxDistribSaved.get(key);
            }

            //si plusieurs valeur de domaine elles ont la même prob on recupere la prob du premier
            AbstractDouble maxValue = max.values().iterator().next();

            maxValue = maxValue.multiply(obsParentState.getProbabilityForCurrentValue());

            if(maxValue.compareTo(hiddenVarMax) > 0){

                hiddenVarMax = maxValue;
            }

            AbstractDouble forward = distrib.get(domainValue).divide(distrib.get(totalDomainValues));
            //on multiplie le resultat du filtre pour la variable cachée
            mulTransitionForward = mulTransitionForward.multiply(forward);
            //on additionne pour chaque valeur
            hiddenVarSum = hiddenVarSum.add(mulTransitionForward);
        }

        return new ForwardSumRs(hiddenVarSum, hiddenVarMax);
    }

    private class ForwardSumRs {

        public ForwardSumRs(AbstractDouble sum, AbstractDouble max) {

            this.sum = sum;

            this.max = max;
        }

        private AbstractDouble sum, max;
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

    public Map<String, Map<Domain.DomainValue, AbstractDouble>> getMaxDistribSaved() {
        return maxDistribSaved;
    }
}
