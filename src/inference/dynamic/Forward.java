package inference.dynamic;

import domain.Domain;
import domain.data.AbstractDouble;
import network.MegaVariable;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.*;

import static inference.dynamic.Util.*;

public class Forward {

    protected static boolean forwardLog = false;

    protected Map<String, Map<Domain.DomainValue, AbstractDouble>> forwardDistribSaved = new Hashtable<>();

    protected Map<String, Map<Domain.DomainValue, AbstractDouble>> maxDistribSaved = new Hashtable<>();

    protected Map<String, Map<Domain.DomainValue, List<Variable>>> mostLikelyPath = new Hashtable<>();

    protected DynamicBayesianNetwork network;

    public Forward(DynamicBayesianNetwork network) {

        this.network = network;
    }

    /*------------------- PREDICTION--------------------*/

    public AbstractDouble prediction(Variable request, int time) {

        return this.prediction(new LinkedList<Variable>(Arrays.asList(new Variable[]{request})), time);
    }

    public AbstractDouble prediction(List<Variable> requests, int time) {

        //étend le reseau jusqu'au temps voulu
        while (this.network.getTime() < time) {

            this.network.extend();
        }

        List<Variable> requests2 = new LinkedList<>();
        //pour chacune des variables de la requete
        for (Variable req : requests) {
            //récuperer la variable enregitré dans le reseau pour le temps time
            Variable networkVar = this.network.getVariable(time, req);
            //assigner la même valeur que demandé
            networkVar.setDomainValue(req.getDomainValue());

            requests2.add(networkVar);
        }

        return this.filtering(requests2);
    }

    /*------------------- FILTERING--------------------*/

    public AbstractDouble filtering(Variable request) {

        return this.filtering(Arrays.asList(new Variable[]{request}));
    }

    public Map<Domain.DomainValue, AbstractDouble> filteringFull(List<Variable> requests) {

        String key = getDistribSavedKey(requests);

        this.forward(requests, key, 0);

        return this.forwardDistribSaved.get(key);
    }

    public AbstractDouble filtering(List<Variable> requests) {

        Variable megaRequest = requests.size() == 1 ? requests.get(0) : MegaVariable.encapsulate(requests);

        String key = getDistribSavedKey(requests);

        this.forward(requests, key, 0);

        Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(key);

        return distrib.get(megaRequest.getDomainValue()).divide(distrib.get(totalDomainValues));
    }

    /*------------------- FORWARD--------------------*/

    public void forward(Variable request) {

        List<Variable> requests = Arrays.asList(new Variable[]{request});

        this.forward(requests, getDistribSavedKey(requests), 0);
    }

    public void forward(List<Variable> requests, String key, int depth) {

        String ident = Util.getIdent(depth);

        if (forwardLog) {
            ident = getIdent(depth);
            System.out.println();
            System.out.println(ident + "************************************");
            System.out.println(ident + "FORWARD : " + requests + " - KEY : " + key);
            System.out.println(ident + "************************************");
            System.out.println();
        }
        //les variables de requete d'origine doivent avoir le même temps

        //création d'une distribution vide pour chaque valeur de la requete
        //qui peuvent être des combinaisons de valeur si la reqete à plusieurs variables
        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();

        Map<Domain.DomainValue, AbstractDouble> maxDistribution = new Hashtable<>();

        Map<Domain.DomainValue, List<Variable>> mostLikelyPath = new Hashtable<>();

        //total pour toutes les valeurs de la requete
        AbstractDouble totalDistribution = this.network.getDoubleFactory().getNew(0.0);

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
            for (Variable observation : request.getObservations()) {
                //si l'observation est déja connu on passe
                if (requestsObservations.containsKey(observation.getVarTimeId())) continue;
                //on ajoute les observations de la requete
                requestsObservations.put(observation.getVarTimeId(), observation);
                //et enfin les eventuels etats parent des observations
                //utile pour les calculer mais qui ne seraient pas present dans la requete
                for (Variable dep : observation.getDependencies()) {

                    fullRequest.put(dep.getVarTimeId(), dep);
                }
            }
        }

        //ensuite on recherche les variables qui sont au temps initial elles n'ont ni observations ni parent
        //et doivent être traité differrement par exemple une requete qui contient 1 ou plus variable de temps 0
        //ou une chaine de markov d'ordre 2 qui contient des variables à des coupes temporelles différentes
        //les autres seront traités à partir de leur observations
        Set<Variable> requestTime0 = new LinkedHashSet<>();

        for (Variable request : fullRequest.values()) {

            if (request.getTime() == network.getInitTime()) {

                requestTime0.add(request);
            }
        }

        if (forwardLog) {

            System.out.println(ident + "FULL REQUEST " + fullRequest.values());
        }

        Variable megaRequest = requests.size() == 1 ? requests.get(0) : MegaVariable.encapsulate(requests);

        Variable fullMegaRequest = fullRequest.values().size() == 1 ?
                fullRequest.values().iterator().next() :
                MegaVariable.encapsulate(new ArrayList<>(fullRequest.values()));

        Variable megaRequest0 = null;

        if (!requestTime0.isEmpty()) {
            //si pas de variabel en temps 0 on ne fait rien
            //ici ce la créerait une megavariable vide qui poserait problème
            megaRequest0 = requestTime0.size() == 1 ?
                    requestTime0.iterator().next() :
                    MegaVariable.encapsulate(new ArrayList<>(requestTime0));
        }

        //List<List<Domain.DomainValue>> requestsValuesCombinations = BayesianNetwork.domainValuesCombinations(fullRequest.values());

        Domain.DomainValue originalValue = fullMegaRequest.saveDomainValue();

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

        //pour chaque combinaison de valeur de la requete complétée
        for (Domain.DomainValue domainValue : fullMegaRequest.getDomainValues()) {

            fullMegaRequest.setDomainValue(domainValue);

            if (forwardLog) {

                System.out.println();

                System.out.println(ident + "FULL REQUEST COMBINATION : " + fullRequest);
            }
            //initialisation de la multiplication à 1
            AbstractDouble requestValueProbability = this.network.getDoubleFactory().getNew(1.0);

            AbstractDouble requestValueMaxProbability = this.network.getDoubleFactory().getNew(1.0);

            LinkedList<Variable> maxParentStates = new LinkedList<>();
            //on demarre par la partie modele de capteur
            //si on à plusieurs observations chacune est independantes des autres
            //et est calculé separemment suivit de la partie sommation contenant l'appel recursif au forward
            //les variables au temps zero n'ont pas de dependances dont pas d'appel recursif
            for (Variable observation : requestsObservations.values()) {
                //au cas ou la variale d'observation est nul on obtient une prédiction plutot qu'un filtrage
                if (observation.getDomainValue() != null) {

                    AbstractDouble obsProb = observation.getProbabilityForCurrentValue();
                    //valeur du modele de capteur
                    requestValueProbability = requestValueProbability.multiply(obsProb);
                    //idem pour calculer un max pour la sequence la plus vraissemblable
                    requestValueMaxProbability = requestValueMaxProbability.multiply(obsProb);
                    if (forwardLog) {
                        System.out.println();
                        System.out.println(ident + "OBS P(" + observation + "|" + observation.getDependencies() + ") = " + observation.getProbabilityForCurrentValue());
                        System.out.println();
                    }
                }
                //ici une observation peut avoir plusieurs parents et il faut à mon sens
                //les traiter separemment soit une somme par variable parent
                //dont les resultats seront multipliés. Fonctionne pour le cas simple
                //reste à tester sur des exemples plus complexes !
                for (Variable stateObserved : observation.getDependencies()) {
                    //total de la somme sur les valeurs cachées initialisé à zero
                    ForwardSumRs rs = forwardHiddenVarSum(stateObserved, depth + 1);
                    //qu'on multiplie avec les resultat pour les observations precedentes
                    requestValueProbability = requestValueProbability.multiply(rs.sum);
                    //idem pour le maximum qauf que l'on mutpliplie le model de capteur par le maximum sur les variables cachées et non la somme
                    requestValueMaxProbability = requestValueMaxProbability.multiply(rs.max);
                    //ajoute les variables et leur etats qui ont produit le max

                    //la question est : si on à plusieurs observation ainsi que plusieurs états parent observés en t
                    //et qu'au final on obtient des etats situé en t - 1 avec des valeurs differentes
                    //pour differents max obtenus ?
                    //System.out.println(rs.maxDomainVars);
                    maxParentStates.addAll(rs.maxDomainVars);
                }
            }

            if (!requestTime0.isEmpty()) {
                //pour les variables situées au temps 0 ont obtient directement leur probabilité
                AbstractDouble req0Prob = megaRequest0.getProbabilityForCurrentValue();

                requestValueProbability = requestValueProbability.multiply(req0Prob);

                requestValueMaxProbability = requestValueMaxProbability.multiply(req0Prob);

                if (forwardLog) {
                    System.out.println(ident + "STATE_0 P(" + megaRequest0 + ") = " + req0Prob);
                }
            }
            //Si la combinaison sur laquelle on travaille actuellement contient
            //plus de variables que celles requete originale
            //mais cependant necessaires pour le calcul on s'interesse
            //ici uniquement à la combinaison de valeurs pour les variables de la requete d'origine
            //sinon il faudrait retrouver cette combinaison parmis plusieurs autres qui la contiendrait
            //P(X1=v,X2=v) = P(X1=v,X2=v,X3=v) + P(X1=v,X2=v,X3=f)

            Domain.DomainValue requestDomainValue = megaRequest.getDomainValue();

            if (forwardLog) {
                System.out.println(ident + "ORIGINAL REQUEST COMBINATION " + requests + " : " + requestDomainValue
                        + " - total = " + requestValueProbability);
            }

            if (!distribution.containsKey(requestDomainValue)) {
                //enregistrement de la probabilité pour la valeur courante de la requete
                distribution.put(requestDomainValue, requestValueProbability);
                //enregistre le maximum
                maxDistribution.put(requestDomainValue, requestValueMaxProbability);

            } else {
                //enregistrement de la probabilité pour la valeur courante de la requete additionné à la precedente
                //pour une même combinaison
                distribution.put(requestDomainValue, distribution.get(requestDomainValue).add(requestValueProbability));

                maxDistribution.put(requestDomainValue, maxDistribution.get(requestDomainValue).add(requestValueMaxProbability));
            }

            //pour chaque combinaison de valeur de la requete enregistre
            //la liste des variables et leur valeurs pour celle qui offre le max.
            mostLikelyPath.put(requestDomainValue, maxParentStates);

            totalDistribution = totalDistribution.add(requestValueProbability);
        }
        //enregistre les totaux pour toutes les combinaisons
        //pour les maximum inutile d'enregistrer les totaux
        //etant donné que normaliser ne change pas le rapport de grandeur entre les valeurs
        distribution.put(totalDomainValues, totalDistribution);

        fullMegaRequest.setDomainValue(originalValue);

        this.maxDistribSaved.put(key, maxDistribution);

        this.forwardDistribSaved.put(key, distribution);

        this.mostLikelyPath.put(key, mostLikelyPath);
    }

    protected ForwardSumRs forwardHiddenVarSum(Variable obsParentState, int depth) {

        String ident = Util.getIdent(depth);

        if (forwardLog) {

            ident = getIdent(depth);
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

        Variable megaHiddenVar = obsParentState.getDependencies().size() == 1 ? obsParentState.getDependencies().get(0) :
                MegaVariable.encapsulate(obsParentState.getDependencies());

        Domain.DomainValue originalValue = megaHiddenVar.saveDomainValue();

        AbstractDouble hiddenVarMax = this.network.getDoubleFactory().getNew(0.0);

        List<Variable> maxHiddenvars = null;

        AbstractDouble hiddenVarsSum = this.network.getDoubleFactory().getNew(0.0);
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


        //List<List<Domain.DomainValue>> hiddenVarsCombinations = BayesianNetwork.domainValuesCombinationsCheckInit(obsParentState.getDependencies());

        String key = getDistribSavedKey(obsParentState.getDependencies());

        // System.out.println(ident+"COMBINATION check init : "+ megaHiddenVar.getDomainValuesCheckInit());

        List<Domain.DomainValue> domainValues = megaHiddenVar.getDomainValuesCheckInit();

        //pour chaque combinaison de valeurs
        for (Domain.DomainValue domainValue : domainValues) {

            megaHiddenVar.setDomainValue(domainValue);

            //début de la multiplication avec la valeur fourni par le modele de transition
            AbstractDouble mulTransitionForward = obsParentState.getProbabilityForCurrentValue();
            //on a ici potentiellement une filtrage sur plusieurs variable si plusieurs variables cachées
            if (forwardLog) {
                System.out.println(ident + "SUM COMBINAISON : " + obsParentState.getDependencies());
                System.out.println(ident + "TRANSITION P(" + obsParentState + "|" + obsParentState.getDependencies() + ") = " + obsParentState.getProbabilityForCurrentValue());
            }

            Map<Domain.DomainValue, AbstractDouble> distrib = this.forwardDistribSaved.get(key);

            Map<Domain.DomainValue, AbstractDouble> max = this.maxDistribSaved.get(key);

            if (distrib == null) {

                this.forward(obsParentState.getDependencies(), key, depth + 1);

                max = this.maxDistribSaved.get(key);

                distrib = this.forwardDistribSaved.get(key);

                if (forwardLog) {
                    System.out.println(ident + "FORWARD REC : " + distrib.get(domainValue));
                }

            } else {

                if (forwardLog) {
                    System.out.println(ident + "FORWARD SAVED : " + distrib.get(domainValue));
                }
            }
            //maximum pour une certain valeur de la ou les variables cachées
            //on ne s'interesse pas au maximum sur toutes les valeurs
            //mais bien à une probabilité max donnée parmis l'ensemble des valeurs que peut prendre de la variable cachée
            AbstractDouble maxValue = max.get(domainValue);
            //ce n'est qu'en multipliant cette probabilité par celle de la requete sachant les variable cachées
            //pour une combinaison de valeurs donnée que l'on obtient le chemin max.

            maxValue = maxValue.multiply(obsParentState.getProbabilityForCurrentValue());

            if (maxValue.compareTo(hiddenVarMax) > 0) {

                hiddenVarMax = maxValue;
                //sauvegarde l'état des variables cachées pour le maximum
                //les valeurs pouvant encore changer dans la procedure
                List<Variable> copyDependencies = new LinkedList<>();

                for (Variable dependencie : obsParentState.getDependencies()) {

                    copyDependencies.add(dependencie.copyLabelTimeValueDom());
                }

                maxHiddenvars = copyDependencies;
            }

            //recupere le resultat du forward pour la combinaison de valeurs courantes des variables cachées
            AbstractDouble forward = distrib.get(domainValue).divide(distrib.get(totalDomainValues));
            //on multiplie le resultat du forward avec la partie transition
            mulTransitionForward = mulTransitionForward.multiply(forward);
            //on additionne pour chaque combinaison de valeur pour les variables cachées
            hiddenVarsSum = hiddenVarsSum.add(mulTransitionForward);
        }

        megaHiddenVar.setDomainValue(originalValue);

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


    /*------------------------------- VIEW ----------------------------------*/


    public void showForward() {

        showDistributions("FORWARD", this.forwardDistribSaved);
    }

    public void showForwardDistributions() {

        System.out.println("===========FORWARD============");

        showDynamicDistributions(this.forwardDistribSaved);
    }

    public void showMaxDistributions() {

        showDynamicDistributions(this.maxDistribSaved);
    }

    public List<Variable> computeMostLikelyPath(Variable... request) {

        return computeMostLikelyPath(Arrays.asList(request));
    }

    public List<Variable> computeMostLikelyPath(List<Variable> requests) {

        Variable megaRequest = requests.size() == 1 ? requests.get(0) : MegaVariable.encapsulate(requests);
        //la sequence d'états la plus probable est calculée à partir d'une liste d'états
        //de 0 à un temps t. la liste des variables et leur ordre doit être le même
        //que lors du premier appel à la methode forward
        //on récupere la clé correspondant aux variables
       // String key = Util.getDistribSavedKey(requests);

        String key = megaRequest.getVarTimeId();

        Map<Domain.DomainValue, AbstractDouble> maxProbs = this.maxDistribSaved.get(key);

        Map<Domain.DomainValue, List<Variable>> maxPath = this.mostLikelyPath.get(key);

        Domain.DomainValue maxValue = null;

        AbstractDouble max = network.getDoubleFactory().getNew(0.0);
        //on calcul la combinaison de valeur pour la requete
        //ayant la plus grande probabilité
        for (Domain.DomainValue value : maxProbs.keySet()) {

            if (maxProbs.get(value).compareTo(max) > 0) {

                max = maxProbs.get(value);

                maxValue = value;
            }
        }

        megaRequest.setDomainValue(maxValue);

        //ajoute la liste des états initialisés en début de liste
        List<Variable> mostLikelyPath = new LinkedList();

        mostLikelyPath.add(megaRequest);
        //charge les états suivants en commencant par recuperer la liste des variables
        //parents de la requete avec les valeurs de domaines ayant donné le maximum
        this.loadMostLikelyPath(mostLikelyPath, maxPath.get(maxValue));

        return mostLikelyPath;
    }

    protected void loadMostLikelyPath(List<Variable> mostLikelyPath, List<Variable> maxVars) {

        //en time = 0 la liste est vide
        if (maxVars.isEmpty()) {

            return;
        }

        Variable megaMaxVar = maxVars.size() == 1 ? maxVars.get(0) : MegaVariable.encapsulate(maxVars);

        //ajoute les variables courantes avec leurs valeurs de domaine
        mostLikelyPath.add(megaMaxVar);
        //récupère la clé correspondant à la liste de variables courantes

        this.loadMostLikelyPath(mostLikelyPath, this.mostLikelyPath.get(megaMaxVar.getVarTimeId()).get(megaMaxVar.getDomainValue()));
    }

    /*------------------------------- GETTER SETTER ----------------------------------*/


    public Map<String, Map<Domain.DomainValue, AbstractDouble>> getForwardDistribSaved() {
        return forwardDistribSaved;
    }

    public Map<String, Map<Domain.DomainValue, AbstractDouble>> getMaxDistribSaved() {
        return maxDistribSaved;
    }

    public Map<String, Map<Domain.DomainValue, List<Variable>>> getMostLikelyPath() {
        return mostLikelyPath;
    }
}
