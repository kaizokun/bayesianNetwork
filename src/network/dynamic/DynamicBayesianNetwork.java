package network.dynamic;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import javafx.scene.chart.ValueAxis;
import network.BayesianNetwork;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.Model.Dependency;

import java.util.*;

import static network.BayesianNetwork.requestValuesCombinations;

public class DynamicBayesianNetwork extends BayesianNetwork {

    protected static boolean backwardLog = false, forwardLog = true;

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

    private Map<String, Map<Domain.DomainValue, List<Variable>>> mostLikelyPath = new Hashtable<>();

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

    public void forwardBackward(List<Variable> requests) {

        this.forwardBackward(requests, 0, this.time, this.time);
    }

    public void forwardBackward(List<Variable> requests, int smootStart, int smootEnd) {

        this.forwardBackward(requests, smootStart, smootEnd, this.time);
    }

    public void forwardBackward(List<Variable> requests, int smootStart, int smootEnd, int backWardEnd) {

        /*
         * smootStart : debut de l'intervalle pour les variable à lisser
         * smootEnd : fin de l'intervalle
         *
         * exemple : on souhaite lisser entre 5 et 8 avec un temps courant de 10
         *
         * il faut calculer le forward sur l'intervalle 0 à 5 le forward etant initialisé au temps 0
         * et calculer le backward sur l'intervalle 10 à 5 le backward etant initialisé au temps final
         * bien qu'il pourrait l'être depuis un temps précédent ici backWardEnd.
         *
         * */

        //on travaille sur les variables de la requete situées au temps smootEnd
        List<Variable> startForwardVars = new LinkedList<>();
        //variables situées au temps T
        for (Variable req : requests) {

            startForwardVars.add(this.getVariable(smootEnd, req));
        }
        //on travaille sur les variables de la requete situées au temps smootStart
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

        for (int time = smootStart; time <= smootEnd; time++) {

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

        forwardBackward(new LinkedList<Variable>(Arrays.asList(new Variable[]{request})),
                smootStart, smootEnd, backWardEnd);
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

        return this.smoothing(new LinkedList<Variable>(Arrays.asList(new Variable[]{request})), timeEnd);
    }



    /*------------------- BACKWARD --------------------*/

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


    public void backWard(List<Variable> requests, String key, int depth, int timeEnd) {

        //System.out.println(this.getIdent(depth)+" BACKWARD "+request);

        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();

        //atteind une requete située à la derniere phase temporelle si plusieurs elles doivent être au même temps
        if (requests.get(0).getTime() == timeEnd) {

            List<List<Domain.DomainValue>> domainValuesLists = requestValuesCombinations(requests);

            for (List<Domain.DomainValue> domainValues : domainValuesLists) {

                distribution.put(new Domain.DomainValue(domainValues), doubleFactory.getNew(1.0));
            }
            distribution.put(totalDomainValues, doubleFactory.getNew(1.0));

            this.backwardDistribSaved.put(key, distribution);

            return;
        }

        /*
        *
        * Chaine de markov d'ordre 2
        *
        *               ________________
        *    __________|_____           |
        *   |          |     |          |
        *   |          |     V          V
        * (s(0))-->(s(1))-->(s(2))-->(s(3))
        *             |        |        |
        *             V        V        V
        *          (o(1))   (o(2))   (o(3))
        *
        * le calcul est un peu plus delicat dans ce cas, par exemple :
        *
        * on à 4 états s(0) , s(1)|s(0) , s(2)|s(1),s(0) , s(3)|s(2),s(1)
        * 3 observations o(1)|s(1)  o(2)|s(2) o(3)|s(3)
        *
        * la requete est s(1), l'observation suivante o(2), l'état parent de l'observation qui est la variable cachée
        * et s(2) et qui à deux parents s(1),s(0) par consequent on complete la requete par s(0)
        * on travaille donc sur des combinaisons de valeurs pour s(1),s(0) en dehors de la somme,
        * puis dans la 3eme partie de la somme (qui se fait sur les valeurs prises par s(2))
        * s(2) est calculée par rapport à une assignation de valeur fixes à s(1),s(0).
        * Maintenant la 2eme partie récursive ou s(2) devient la requete, on recommence le processus
        * et cette fois ci la requete est complétée par s(1), si on ne tiens pas compte que s(1)
        * à déja une valeur initialisée precedemment on va calculer une distribution sur s(2) pour toutes les valeurs
        * de s(1) ce qui n'est pas cohérent. il faut donc soit des combinaisons sur s(1) et s(2) ou la valeur de s(1) ne change pas
        * ce qui revient à boucler sur les valeurs de s(2). On pourrait aussi ignorer les variables déja initialisées
        * lorsqu'on complete la requete, en effet ici le but de completer la requete est de pouvoir calculer s(3)|s(2),s(1)
        * hors s(1) ayant déja une valeur obtenu lors d'un appel recursif précédent on à pas de problème pour obtenir sa valeur.
        *
        * Mais cela pose un autre problème (encore) si on calcule est sauvegarde la distribution sur s(2) on en l'aura que
        * par raport à une seule valeur de s(1) et la partie dynamique echoue !
        *
        * il faudrait dans ce cas si calculer une distribution sur s(2) et s(1), soit la requete complétée,
        * et ensuite recuperer le bon résultat pré calculé, à chaque itération sur une valeur de s(2)
        * pour une chaine d'ordre x on s'interesse donc à la variable s et à la même variable s sur les temps précédents
        * sur une profondeur x - 1. POur une chaine de markov 1 ca donnerait uniquement s(2), d'ordre 2, s(2) et s(1)
        *
        * apres pour le lissage même si on doit calculer une distribution sur une requete complétée
        * on peut toujours moyenner sur le(s) parents pour multiplier avec le forward. à voir dans le détail.
        *
        * */

        //récuperation des observations de la variable au temps suivant
        Map<String, Variable> nextObservations = new Hashtable<>();

        Map<String, Variable> fullRequest = new Hashtable<>();

        for (Variable request : requests) {
            //ajout des requetes dans la liste complétée
            fullRequest.put(request.getVarTimeId(), request);
            //récupération de la variable au temps suivant
            //faut il recuperer les variables deux temps en avant ?
            Variable nextRequest = this.getVariable(request.getTime() + 1, request);
            //si variable en temps t aucune ne suit
            if(nextRequest != null) {

                for (Variable nextObservation : nextRequest.getObservations()) {
                    //ignore les observation déja enregistrées
                    if(nextObservations.containsKey(nextObservation.getVarTimeId())) continue;

                    nextObservations.put(nextObservation.getVarTimeId(), nextObservation);
                    //maintenant chaque observation sera calculé en fonction de ses parents dans partie 1 de la somme
                    for(Variable observationDep : nextObservation.getDependencies()){
                        //et le parent de l'observation sera calculé en fonction des siens en partie 3
                        //cependant certain pourrait ne pas faire partie de la requete originale
                        //exemple pour une chaine de markov d'ordre 2
                        //une requete sur s(1), on prend l'observation suivante o(2)
                        //o(2) à pour parent s(2) qui depend de s(1) mais aussi de s(0)
                        //donc la partie 3 à besoin d'assigner une valeur à s(0) et s(1) pour calculer s(2)
                        //qui doit completer la requete même si dans la distribution finale
                        //on ne prend en compte que s(1) pour ensuite faire la multiplication
                        //avec le forward qui est une distribution sur s(1) également
                        for(Variable missingRequest : observationDep.getDependencies()){

                            fullRequest.put(missingRequest.getVarTimeId(), missingRequest);
                        }
                    }
                }
            }
        }



        List<Domain.DomainValue> originalValues = new LinkedList<>();

        for (Variable request : requests) {

            originalValues.add(request.getDomainValue());
        }

        List<List<Domain.DomainValue>> domainValuesLists = requestValuesCombinations(fullRequest.values());

        AbstractDouble totalRequest = doubleFactory.getNew(0.0);
        //pour combinaison de valeur de la requete
        for (List<Domain.DomainValue> requestValue : domainValuesLists) {

            Iterator<Variable> requestsIterator = fullRequest.values().iterator();

            for (Domain.DomainValue domainValue : requestValue) {

                requestsIterator.next().setDomainValue(domainValue);
            }

            //multiplier le resultat pour chaque observation
            AbstractDouble multiplyObservations = doubleFactory.getNew(1.0);

            for (Variable nextObservation : nextObservations.values()) {

                multiplyObservations = multiplyObservations.multiply(
                        backwardSum(nextObservation, depth, timeEnd));

            }

            List<Domain.DomainValue> requestDomainValues = new LinkedList<>();

            for (Variable request : requests) {

                requestDomainValues.add(request.getDomainValue());
            }

            Domain.DomainValue domainValuesCombi = new Domain.DomainValue(requestDomainValues);

            if (!distribution.containsKey(domainValuesCombi)) {
                //enregistrement de la probabilité pour la valeur courante de la requete
                distribution.put(domainValuesCombi, multiplyObservations);

            } else {
                //enregistrement de la probabilité pour la valeur courante de la requete additionné à la precedente
                //pour une même combinaison
                distribution.put(domainValuesCombi, distribution.get(domainValuesCombi).add(multiplyObservations));
            }

            totalRequest = totalRequest.add(multiplyObservations);
        }

        distribution.put(totalDomainValues, totalRequest);

        Iterator<Variable> requestIterator = requests.iterator();
        //restaure les valeur originales
        for (Domain.DomainValue domainValue : originalValues) {

            requestIterator.next().setDomainValue(domainValue);
        }

        this.backwardDistribSaved.put(key, distribution);
    }

    public AbstractDouble backwardSum(Variable nextObservation, int depth, int timeEnd) {

        AbstractDouble sum = doubleFactory.getNew(0.0);

        List<List<Domain.DomainValue>> hiddenVarsValues = requestValuesCombinations(nextObservation.getDependencies());

        String key = getDistribSavedKey(nextObservation.getDependencies());

        for (List<Domain.DomainValue> domainValues : hiddenVarsValues) {

            AbstractDouble multiplyUnderSum = doubleFactory.getNew(1.0);

            Iterator<Variable> obsDependenciesIterator = nextObservation.getDependencies().iterator();

            for (Domain.DomainValue depValue : domainValues) {

                obsDependenciesIterator.next().setDomainValue(depValue);
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

               // if (backwardLog) System.out.print( hiddenVar +" "+hiddenVar.getDependencies());

                if (backwardLog) System.out.print(" * " + hiddenVar.getProbabilityForCurrentValue());

                multiplyUnderSum = multiplyUnderSum.multiply(hiddenVar.getProbabilityForCurrentValue());
            }

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

        return this.prediction(new LinkedList<Variable>(Arrays.asList(new Variable[]{request})), time);
    }

    /*------------------- FILTERING--------------------*/

    public AbstractDouble filtering(Variable request) {

        return this.filtering(new LinkedList<Variable>(Arrays.asList(new Variable[]{request})));
    }

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

    /*------------------- FORWARD--------------------*/

    private void forward(Variable request, String key, int depth) {

        List<Variable> requests = new LinkedList<Variable>(Arrays.asList(new Variable[]{request}));

        this.forward(requests, getDistribSavedKey(requests), 0);
    }

    private void forward(List<Variable> requests, String key, int depth) {

        String ident = "";

        if(forwardLog) {
            ident = getIdent(depth);
            System.out.println();
            System.out.println(ident + "************************************");
            System.out.println(ident + "FORWARD : " + requests + " - KEY : " + key);
            System.out.println(ident + "************************************");
            System.out.println();
            maxDepth = Math.max(maxDepth, depth);
        }
        //les variables de requete d'origine doivent avoir le même temps

        //création d'une distribution vide pour chaque valeur de la requete
        //qui peuvent être des combinaisons de valeur si la reqete à plusieurs variables
        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();

        Map<Domain.DomainValue, AbstractDouble> maxDistribution = new Hashtable<>();

        Map<Domain.DomainValue, List<Variable>> mostLikelyPath = new Hashtable<>();

        List<Domain.DomainValue> originalValues = new LinkedList<>();

        for (Variable request : requests) {

            originalValues.add(request.getDomainValue());
            //rend la valeur de la variable nule
            request.clear();
        }

        //total pour toutes les valeurs de la requete
        AbstractDouble totalDistribution = this.doubleFactory.getNew(0.0);

        //max pour toutes les valeurs de la requete
        AbstractDouble totalMax = this.doubleFactory.getNew(0.0);

        //liste des observations à traiter pour l'ensemble de la requete
        //un etat peut avoir plusieurs observations par exemple une maladie plusieurs symptomes
        //un symptomes peut avoir plusieurs états parent, un symptome identiques pour plusieurs états différents
        //!differencier les observation en fonction du temps la classe variable ne se base que sur le label

        //certaines variables parents des observations pourraient ne pas faire parti de la requete
        //et doivent donc être ajoutés sans cela impossible de calculer la valeur de l'observation
        //si un de ses parents n'a pas de valeur on complete donc la requete si necessaire

        Map<String, Variable> requestsObservations = new Hashtable<>();

        Map<String, Variable> fullRequest = new Hashtable<>();
        //pour chaque variable de la requete
        for (Variable request : requests) {
            //on ajoute la variable dans la liste complétée
            fullRequest.put(request.getVarTimeId(), request);
            //pour chaque observation des variables de la requete
            for(Variable observation : request.getObservations()){
                //si l'observation est déja connu on passe
                if(requestsObservations.containsKey(observation.getVarTimeId())) continue;
                //on ajoute les observations de la requete
                requestsObservations.put(observation.getVarTimeId(), observation);
                //et enfin les eventuels etats parent des observations
                //utile pour les calculer mais qui ne seraient pas present dans la requete
                for(Variable dep : observation.getDependencies()){

                    fullRequest.put(dep.getVarTimeId(), dep);
                }
            }
        }

        //ensuite on recherche les variables qui sont au temps 0 elles n'ont ni observations ni parent
        //et doivent être traité differrement par exemple une requete qui contient 1 ou plus variable de temps 0
        //ou une chaine de markov d'ordre 2 qui contient des variables à des coupes temporelles différentes
        //les autres seront traités à partir de leur observations
        Set<Variable> requestTime0 = new LinkedHashSet<>();

        for(Variable request : fullRequest.values()){

            if(request.getTime() == 0){

                requestTime0.add(request);
            }
        }

        if(forwardLog) {
            System.out.println(ident + "FULL REQUEST " + fullRequest.values());
        }

        List<List<Domain.DomainValue>> requestsValuesCombinations = requestValuesCombinations(fullRequest.values());

        /*
        * Pour le cas ou il faut calculer la sequence d'etats la plus vraissemblable
        * --------------------------------------------------------------------------
        *
        * on pourrait rechercher la sequence la plus vraissemblable pour un sous ensemble d'états
        * de 0 à une certaine coupe temporelle
        *
        * le premier appel à cette méthode se fait donc avec ce sous ensemble d'états
        * ici les variables ne doivent pas être initialisés et comme pour le filtrage
        * on travaille sur des combinaisons de valeurs si plus d'une variable.
        * D'abord de manière analogue au filtrage il faut eventuellement completer les variables de la requete
        * par les parents des observations non compris.
        *
        * Ensuite pour chaque combinaison de valeur de la requete courante situé à un temps t
        * on obtient un ou plusieurs combinaisons de valeurs, par somme (ou max), pour les variables
        * états parents situées à un temps t - 1
        *
        * Si on prend un cas simple ou on à une variable requete t, une observation unique
        * et un max pour un état d'une variable parent unique t - 1
        *
        * Pour chaque combinaison de valeur de la requete
        * il faut associer celle des parents ayant donné le maximum
        * */

        AbstractDouble bestMax = doubleFactory.getNew(0.0);

        List<Variable> fullMaxParentsStates = new LinkedList<>();

        //pour chaque combinaison de valeur de la requete complétée
        for (List<Domain.DomainValue> domainValues : requestsValuesCombinations) {

            Iterator<Variable> requestIterator = fullRequest.values().iterator();
            //initialise les variables avec une combinaison
            for (Domain.DomainValue domainValue : domainValues) {

                requestIterator.next().setDomainValue(domainValue);
            }
            if(forwardLog) {
                System.out.println();
                System.out.println(ident + "FULL REQUEST COMBINATION : " + fullRequest);
            }
            //initialisation de la multiplication à 1
            AbstractDouble requestValueProbability = this.doubleFactory.getNew(1.0);

            AbstractDouble requestValueMaxProbability = this.doubleFactory.getNew(1.0);

            LinkedList<Variable> maxParentStates = new LinkedList<>();
            //on demarre par la partie modele de capteur
            //si on à plusieurs observations chacune est independantes des autres
            //et est calculé separemment suivit de la partie sommation contenant l'appel recursif au forward
            //les variables au temps zero n'ont pas de dependances dont pas d'appel recursif
            //
            for (Variable observation : requestsObservations.values()) {
                //au cas ou la variale d'observation est nul on obtient une prédiction plutot qu'un filtrage
                if (observation.getDomainValue() != null) {
                    //valeur du modele de capteur
                    requestValueProbability = requestValueProbability.multiply(observation.getProbabilityForCurrentValue());
                    //idem pour calculer un max pour la sequence la plus vraissemblable
                    requestValueMaxProbability = requestValueMaxProbability.multiply(observation.getProbabilityForCurrentValue());
                    if(forwardLog) {
                        System.out.println();
                        System.out.println(ident + "OBS P(" + observation + "|" + observation.getDependencies() + ") = " + observation.getProbabilityForCurrentValue());
                        System.out.println();
                    }
                }
                //ici une observation peut avoir plusieurs parents et il faut à mon sens
                //les traiter separemment soit une somme par variable parent
                //dont les resultats seront multipliés. marche pour le cas simple
                //reste à tester sur des exemples plus complexes !
                for (Variable stateObserved : observation.getDependencies()) {
                    //total de la somme sur les valeurs cachées initialisé à zero
                    ForwardSumRs rs = forwardHiddenVarSum(stateObserved, depth + 1);
                    //qu'on multiplie avec les resultat pour les observations precedentes
                    requestValueProbability = requestValueProbability.multiply(rs.sum);
                    //idem pour le maximum qauf que l'on mutpliplie le model de capteur par le maximum sur les variables cachées et non la somme
                    requestValueMaxProbability = requestValueMaxProbability.multiply(rs.max);
                    //ajoute les variables et leur etats qui ont produit le max

                    //la question est si on à plusieurs observation ainsi que plusieurs états parent observés en t
                    //et qu'au final on obtient des etats situé en t - 1 avec des valeurs differentes
                    //pour differents max obtenus ?
                    maxParentStates.addAll(rs.maxDomainVars);
                }
            }
            //pour les variables situées au temps 0 ont obtient directement leur probabilité
            for(Variable request0 : requestTime0){

                requestValueProbability = requestValueProbability.multiply(request0.getProbabilityForCurrentValue());
                if(forwardLog) {
                    System.out.println(ident + "STATE_0 P(" + request0 + ") = " + request0.getProbabilityForCurrentValue());
                }
            }
            //Si la combinaison sur laquelle ont travaille actuellement contient
            //plus de variables que celles requete originale
            //mais cependant necessaires pour le calcul on s'interesse
            //ici uniquement à la combinaison de valeurs pour les variables de la requete d'origine
            //sinon il faudrait retrouver cette combinaison parmis plusieurs autres qui la contiendrait
            //P(X1=v,X2=v) = P(X1=v,X2=v,X3=v) + P(X1=v,X2=v,X3=f)
            List<Domain.DomainValue> requestDomainValues = new LinkedList<>();

            for (Variable request : requests) {

                requestDomainValues.add(request.getDomainValue());
            }

            if(forwardLog) {
                System.out.println(ident + "ORIGINAL REQUEST COMBINATION " + requests + " : " + requestDomainValues
                        + " - total = " + requestValueProbability);
            }

            Domain.DomainValue domainValuesCombi = new Domain.DomainValue(requestDomainValues);

            if (!distribution.containsKey(domainValuesCombi)) {
                //enregistrement de la probabilité pour la valeur courante de la requete
                distribution.put(domainValuesCombi, requestValueProbability);
                //enregistre le maximum
                maxDistribution.put(domainValuesCombi, requestValueMaxProbability);

            } else {
                //enregistrement de la probabilité pour la valeur courante de la requete additionné à la precedente
                //pour une même combinaison
                distribution.put(domainValuesCombi, distribution.get(domainValuesCombi).add(requestValueProbability));

                maxDistribution.put(domainValuesCombi, maxDistribution.get(domainValuesCombi).add(requestValueMaxProbability));
            }

            mostLikelyPath.put(domainValuesCombi, maxParentStates);

            totalDistribution = totalDistribution.add(requestValueProbability);
        }
        //enregistre les totaux pour toutes les combinaisons
        //pour les maximum inutile d'enregistrer les totaux
        //etant donné que normaliser ne change pas le rapport de grandeur entre les valeurs
        distribution.put(totalDomainValues, totalDistribution);

        Iterator<Variable> requestIterator = requests.iterator();
        //restaure les valeur originales
        for (Domain.DomainValue domainValue : originalValues) {

            requestIterator.next().setDomainValue(domainValue);
        }

        this.maxDistribSaved.put(key, maxDistribution);

        this.forwardDistribSaved.put(key, distribution);

        this.mostLikelyPath.put(key, mostLikelyPath);
    }

    private ForwardSumRs forwardHiddenVarSum(Variable obsParentState, int depth) {

        String ident = "";
        if(forwardLog) {
            ident = this.getIdent(depth);

            System.out.println(ident + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println(ident + "SUM ON " + obsParentState + " DEPENDENCIES : " + obsParentState.getDependencies());
            System.out.println(ident + "++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println();
        }
        //avant de demarrer la sommation les variables parent de "obsParentState" non initialisées doivent être reseter à la fin
        //car si elles conservent leur valeur la recuperation des combinaison echoue pour celle initialisées
        //lors d'un rappel de la sommation pour une combinaison differente de "obsParentState" dans la procedure appelante
        //par contre si une variabel parent de "obsParentState" est déja initialisé ce qui peut être le cas
        //si elle fait déja partie de la requete dans la procedure appelante, elle doit rester en l'état

        List<Variable> varsToReset = new LinkedList<>();

        for(Variable dep : obsParentState.getDependencies()){

            if(!dep.isInit()){

                varsToReset.add(dep);
            }
        }

        AbstractDouble hiddenVarMax = this.doubleFactory.getNew(0.0);

        List<Variable> maxHiddenvars = null;

        AbstractDouble hiddenVarsSum = this.doubleFactory.getNew(0.0);
        //une variable de la requete peut avoir plusieurs parents il faut donc récuperer les combinaisons de valeurs
        //pour sommer sur chacune d'entre elle.
        //CAS TRES SPECIFIQUES POUR LES CHAINES DE MARKOV DE NIVEAU SUPERIEUR A 1
        //les variables deja initialisé gardent la même valeur et on ne somme pas sur les autres
        //exemple si la requete dans la procedure forward appelante contient deux variable dont l'une
        //est parent de l'autre par exemple pour une chaine de markov d'ordre 2
        //une variable s(2) à pour parent s(1) et s(0) par contre s(1) à uniquement pour parent s(0)
        //au moment de calculer dans la boucle de sommation p( s(2)|s(1),s(0) ) on boucle sur les combinaisons de valeur pour s(1),s(0)
        //mais ce n'est pas le problème, l'erreur se situe plutot au niveau du rappel de la methode forward
        //car on a egalement un appel recursif à forward pour s(1),s(0) qui devient la requete. On va par consequent cette fois si
        //calculer une distribution pour cette combinaison de variable et donc assigner leur assigner des valeurs, ici 4 combinaison pour des variables booleenes
        //on calcule ensuite une observation de s(1) par exemple o(1)|s(1) suivit d'une sommation ( dans cette sous procedure)
        //sur le parent de s(1) ici s(0) qui possede deja une valeur.
        //initialisé precedemment dans la partie forward lorsqu'on boucle sur les combinaisons de la requete s(1),s(0)
        //la sommation ne doit donc se faire que sur la valeur de s(0) déja assignée, si on avait plusieurs parents pour s(1) on aurait des combinaison
        //de valeur ou celle de s(0) serait fixe. Car il me semble que s(0) ne peut être considérée comme variable caché dans ce cas ci.
        //()
        //une version precedente de l'algorithme ne prennait pas ce cas en compte et calculait le forward pour s(1),s(0)
        //separemment pour ensuite combiner les distributions obtenues. Ce qui pose problème pour s(1) car sa distribution
        //était calculée en moyenne par rapport à s(0) puis combiné à une distribution sur s(0), pour cela
        //on prennait toutes les combinaisons pour les valeur de s(1) et s(0), on recuperer les valeurs dans leur distributions
        //respectives, et on multiplier les resultats pour charger une nouvelle distribution dont les clé etait ces combinaison de valeurs
        //on multiplié donc des valeur pour s(1) moyenné sur s(0) = v et s(0) = f à des valeurs s(0) = v
        //la procedure de la sequence la plus probable permet de se rendre compte de l'incoherence de cette methode.
        //Ca ne pose pas de problème pour une requete avec des variables
        //situées dans la même coupe temporelles ou tout leur parent sont des variables cachées

        List<List<Domain.DomainValue>> hiddenVarsCombinations = requestValuesCombinationsCheckInit(obsParentState.getDependencies());

        String key = getDistribSavedKey(obsParentState.getDependencies());

        //pour chaque combinaison de valeurs
        for (List<Domain.DomainValue> domainValues : hiddenVarsCombinations) {

            int j = 0;
            //initialise chaque variable caché avec une valeur de la combinaison
            for (Variable hiddenVar : obsParentState.getDependencies()) {

                hiddenVar.setDomainValue(domainValues.get(j++));
            }

            //début de la multiplication avec la valeur fourni par le modele de transition
            AbstractDouble mulTransitionForward = obsParentState.getProbabilityForCurrentValue();
            //on a ici potentiellement une filtrage sur plusieurs variable si plusieurs variables cachées
            if(forwardLog) {
                System.out.println(ident+"SUM COMBINAISON : "+obsParentState.getDependencies());
                System.out.println(ident+"TRANSITION P("+obsParentState+"|"+obsParentState.getDependencies()+") = "+obsParentState.getProbabilityForCurrentValue());
            }

            Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(key);

            Map<Domain.DomainValue, AbstractDouble> max = this.maxDistribSaved.get(key);

            if (distrib == null) {

                this.forward(obsParentState.getDependencies(), key, depth + 1);

                max = this.maxDistribSaved.get(key);

                distrib = this.forwardDistribSaved.get(key);

                if(forwardLog) {
                    System.out.println(ident + "FORWARD REC : " + distrib.get(new Domain.DomainValue(domainValues)));
                }

            }else{

                if(forwardLog) {
                    System.out.println(ident + "FORWARD SAVED : " + distrib.get(new Domain.DomainValue(domainValues)));
                }
            }
            //probabilité du chemin le plus vraissemblable vers l'état precedent pour une certaine valeur
            AbstractDouble maxValue = max.get(new Domain.DomainValue(domainValues));

            maxValue = maxValue.multiply(obsParentState.getProbabilityForCurrentValue());

            Domain.DomainValue domainvaluesCom = new Domain.DomainValue(domainValues);

            if (maxValue.compareTo(hiddenVarMax) > 0) {

                hiddenVarMax = maxValue;

                List<Variable> copyDependencies = new LinkedList<>();

                for(Variable dependencie : obsParentState.getDependencies()){

                    copyDependencies.add(dependencie.copyLabelTimeValue());
                }

                maxHiddenvars = copyDependencies;
            }

            AbstractDouble forward = distrib.get(domainvaluesCom).divide(distrib.get(totalDomainValues));
            //on multiplie le resultat du filtre pour chaque variable cachée
            mulTransitionForward = mulTransitionForward.multiply(forward);

            //System.out.print(obsParentState.getDependencies()+" "+forward);

            //on additionne pour chaque combinaison de valeur pour les variables cachées
            hiddenVarsSum = hiddenVarsSum.add(mulTransitionForward);
        }
        //remet les valeurs des variables non initialisés à nul
        for(Variable variable : varsToReset){

            variable.clear();
        }

        System.out.println();

        return new ForwardSumRs(hiddenVarsSum, hiddenVarMax, maxHiddenvars, key);
    }

    private class ForwardSumRs {

        public ForwardSumRs(AbstractDouble sum, AbstractDouble max, List<Variable> maxDomainVars, String key) {

            this.sum = sum;

            this.max = max;

            this.maxDomainVars = maxDomainVars;

            this.hiddenVarKey = key;
        }

        private AbstractDouble sum, max;

        private List<Variable> maxDomainVars;

        private String hiddenVarKey;
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

    private String getTreeIdent(int depth) {

        StringBuilder ident = new StringBuilder();

        for (int i = 0; i < depth - 1; i++) {

            ident.append("             ");
        }

        if (depth > 0) {

            ident.append("[____________");
        }

        return ident.toString();
    }

    private String getIdent(int depth) {

        StringBuilder ident = new StringBuilder();

        for (int i = 0; i < depth; i++) {

            ident.append("             ");
        }
        return ident.toString();
    }

    public Map<String, Map<Domain.DomainValue, AbstractDouble>> getMaxDistribSaved() {
        return maxDistribSaved;
    }

    public Map<String, Map<Domain.DomainValue, List<Variable>>> getMostLikelyPath() {
        return mostLikelyPath;
    }
}
