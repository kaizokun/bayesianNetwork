package inference.dynamic;

import domain.Domain;
import domain.data.AbstractDouble;
import environment.Action;
import math.Distribution;
import math.Matrix;
import network.MegaVariable;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.*;

import static inference.dynamic.Util.*;
import static java.util.Arrays.asList;

public class Forward {

    protected Map<String, Distribution> forwardMatrices = new Hashtable<>();

    protected Map<String, Distribution> maxMatrices = new Hashtable<>();

    protected Map<String, Map<Domain.DomainValue, List<Variable>>> mostLikelyPath = new Hashtable<>();

    protected Map<String, Map<Domain.DomainValue, Domain.DomainValue>> mostLikelyPathFull = new Hashtable<>();

    protected DynamicBayesianNetwork network;

    public Forward(DynamicBayesianNetwork network) {

        this.network = network;
    }

    /*------------------- PREDICTION--------------------*/

    public AbstractDouble prediction(Variable request, int time) {

        return this.prediction(new LinkedList(asList(new Variable[]{request})), time);
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

        return this.forwardAsk(requests2);
    }

    /*------------------- FORWARD--------------------*/

    public AbstractDouble forwardAsk(Variable request) {

        Distribution forward = forward(request, false).forward;

        return forward.get(request.getDomainValue()).divide(forward.getTotal());
    }

    public AbstractDouble forwardAsk(List<Variable> request) {

        Variable megaRequest = network.encapsulate(request);

        Domain.DomainValue ask = megaRequest.getDomainValue();

        System.out.println("ASK : " + ask);

        Distribution forward = forward(request, false).forward;

        return forward.get(ask).divide(forward.getTotal());
    }

    public ForwardMax forward(Variable request) {

        return forward(request, false);
    }

    public ForwardMax forward(Variable request, boolean saveDistrib) {

        return forward(asList(new Variable[]{request}), saveDistrib);
    }

    public ForwardMax forward(List<Variable> requests) {

        return forward(requests, false);
    }

    public ForwardMax forward(List<Variable> requests, boolean saveDistribs) {

        return forward(requests, getDistribSavedKey(requests), 0, saveDistribs);
    }

    protected ForwardMax forward(List<Variable> requests, String key, int depth, boolean saveforward) {

        return forward(requests, null, key, depth, saveforward,
                new Hashtable<>(), new Hashtable<>());
    }

    public ForwardMax forward(List<Variable> requests, List<Variable> actions, int timeRequest) {

        return forward(requests, actions, getDistribSavedKey(requests), 0, false,
                new Hashtable<>(), new Hashtable<>());
    }

    public ForwardMax forward(List<Variable> requests, List<Variable> actions, Distribution lastForward, int timeRequest) {

        //Max distribution bogus pour gerer les PDMPO en attendant de reecrire la procedure uniquement pour le PDMPO
        Map<String, Distribution> forwardMatrices = new Hashtable<>(), maxMatrices = new Hashtable<>();

        String key = getDistribSavedKey(requests);

        List<Variable> previousRequest = new LinkedList<>();

        for (Variable request : requests) {
            //recupere les variables pour generer la clé au temps precedent la requete
            //afin d'enregistrer le forward precedent dans la map
            previousRequest.add(this.network.getVariable(timeRequest - 1, request));
        }

        Variable megaPreviousRequest = network.encapsulate(previousRequest);

        Distribution lastMaxBogus = new Distribution(megaPreviousRequest, network.getDoubleFactory());

        String previousKey = getDistribSavedKey(previousRequest);

        maxMatrices.put(previousKey, lastMaxBogus);

        forwardMatrices.put(previousKey, lastForward);

        //System.out.println("PREVIOUS KEY "+previousKey);

        //System.out.println("CURRENT KEY "+key);

        return forward(requests, actions, key, 0, false, forwardMatrices, maxMatrices);
    }

    protected ForwardMax forward(List<Variable> requests, List<Variable> actions, String key, int depth,
                                 boolean saveforward, Map<String, Distribution> forwardMatrices, Map<String, Distribution> maxMatrices ) {

       // System.out.println("FORWARD " + requests + " - depth : " + depth + " - forward : " + forwardMatrices.keySet());

        //les variables de requete d'origine doivent avoir le même temps
        //création d'une distribution vide pour chaque valeur de la requete
        //qui peuvent être des combinaisons de valeur si la requete à plusieurs variables

        Map<Domain.DomainValue, List<Variable>> mostLikelyPath = new Hashtable<>();

        //si pour la requete courante on à plusieurs sommes avec des variables cachées différentes
        //on aura plusieurs distributions differentes
        //  Map<String, Map<Domain.DomainValue, AbstractDouble>> lastForwardDistrib = new Hashtable<>();
        //  Map<String, Map<Domain.DomainValue, AbstractDouble>> lastMaxDistrib = new Hashtable<>();

        //Map<String, Distribution> forwardMatrices = new Hashtable<>();

        //Map<String, Distribution> maxMatrices = new Hashtable<>();

        //total pour toutes les valeurs de la requete
        AbstractDouble totalDistribution = this.network.getDoubleFactory().getNew(0.0);

        //liste des observations à traiter pour l'ensemble de la requete
        //un etat peut avoir plusieurs observations par exemple une maladie plusieurs symptomes
        //un symptomes peut avoir plusieurs états parent, un symptome identiques pour plusieurs états différents
        //il faut differencier les observation en fonction du temps la classe variable ne se base que sur le label
        //dans la méthode hashcode et equal

        //certaines variables parents des observations pourraient ne pas faire parti de la requete
        //et doivent donc être ajoutées sans cela impossible de calculer la valeur de l'observation
        //si un de ses parents n'a pas de valeur. On complete donc la requete si necessaire

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

        //ensuite on recherche les variables qui sont au temps initial. Elles n'ont ni observations ni parent
        //et doivent être traité differrement par exemple une requete qui contient 1 ou plus variable de temps 0
        //ou une chaine de markov d'ordre 2 qui contient des variables à des coupes temporelles différentes
        //les autres seront traités à partir de leur observations
        Set<Variable> requestTime0 = new LinkedHashSet<>();

        for (Variable request : fullRequest.values()) {

            if (request.getTime() == network.getInitTime()) {

                requestTime0.add(request);
            }
        }
        //retire les actions de la distribution initiale
        requestTime0.removeAll(actions);

        //encapsule les variables dans une megavariable si plusieurs afin de
        //gerer de manière polymorphe les variables uniques des listes de variables
        //( gestion des attributions de valeurs etc.)
        Variable megaRequest = network.encapsulate(requests);

        Variable fullMegaRequest = network.encapsulate(new ArrayList<>(fullRequest.values()));

        Variable megaRequest0 = null;

        if (!requestTime0.isEmpty()) {
            //si pas de variable en temps 0 on ne fait rien
            //ici cela créerait une megavariable vide qui poserait problème
            megaRequest0 = network.encapsulate(new ArrayList<>(requestTime0));
        }

        Distribution forwardMatrix = new Distribution(megaRequest, network.getDoubleFactory());

        Distribution maxMatrix = new Distribution(megaRequest, network.getDoubleFactory());

        //valeur originale des variables à restaurer à la fin
        Domain.DomainValue originalValue = fullMegaRequest.saveDomainValue();


        // Pour le cas ou il faut calculer la sequence d'etats la plus vraissemblable
        // --------------------------------------------------------------------------
        //
        // on pourrait rechercher la sequence la plus vraissemblable pour un sous ensemble d'états
        // de 0 à une certaine coupe temporelle
        //
        // le premier appel à cette méthode se fait donc avec ce sous ensemble d'états
        // ici les variables ne doivent pas être initialisés et comme pour le filtrage
        // on travaille sur des combinaisons de valeurs si plus d'une variable.
        // D'abord de manière analogue au filtrage il faut eventuellement completer les variables de la requete
        // par les parents des observations non compris.
        //
        // Ensuite pour chaque combinaison de valeur de la requete courante situé à un temps t
        // on obtient un ou plusieurs combinaisons de valeurs, par somme (ou max), pour les variables
        // états parents situées à un temps t - 1
        //
        // Si on prend un cas simple où on à une variable requete t, une observation unique
        // et un max pour un état d'une variable parent unique t - 1
        //
        // Pour chaque combinaison de valeur de la requete
        // il faut associer celle des parents ayant donné le maximum


        //pour chaque combinaison de valeur de la requete complétée
        for (Domain.DomainValue domainValue : fullMegaRequest.getDomainValues()) {

            fullMegaRequest.setDomainValue(domainValue);

            //initialisation de la multiplication à 1
            AbstractDouble requestValueProbability = this.network.getDoubleFactory().getNew(1.0);

            AbstractDouble requestValueMaxProbability = this.network.getDoubleFactory().getNew(1.0);

            Set<Variable> maxParentStates = new LinkedHashSet<>();
            //on demarre par la partie modele de capteur
            //si on à plusieurs observations chacune est independantes des autres
            //et est calculée separemment suivit de la partie sommation contenant l'appel recursif au forward
            //les variables au temps zero n'ont pas de dependances dont pas d'appel recursif
            //elles sont traitées après cette boucle
            for (Variable observation : requestsObservations.values()) {

                //au cas ou la variale d'observation est nule on obtient une prédiction plutot qu'un filtrage
                //soit pas de mise à jour par l'observation de la valeur obtenu par la somme
                if (observation.getDomainValue() != null) {

                   // System.out.println("OBSERVATION "+observation.getDomainValue());

                    AbstractDouble obsProb = observation.getProbability();
                    //valeur du modele de capteur
                    requestValueProbability = requestValueProbability.multiply(obsProb);
                    //idem pour calculer un max pour la sequence la plus vraissemblable
                    requestValueMaxProbability = requestValueMaxProbability.multiply(obsProb);
                }//else{

                  //  System.out.println("OBSERVATION  NULLE "+observation.getDomainValue());

               // }
                //ici une observation peut avoir plusieurs parents et il faut à mon sens
                //les traiter separemment soit une somme par variable parent
                //dont les resultats seront multipliés. Fonctionne pour le cas simple
                //reste à tester sur des exemples plus complexes !
                for (Variable stateObserved : observation.getDependencies()) {
                    //total de la somme sur les valeurs cachées initialisé à zero
                    ForwardSumRs rs = this.forwardHiddenVarSum(actions, stateObserved, forwardMatrices, maxMatrices,
                            depth + 1, saveforward);
                    //qu'on multiplie avec les resultat pour les observations precedentes
                    requestValueProbability = requestValueProbability.multiply(rs.sum);
                    //idem pour le maximum sauf que l'on multiplie le model de capteur par
                    //le maximum sur les variables cachées et non la somme
                    requestValueMaxProbability = requestValueMaxProbability.multiply(rs.max);

                    //rs.maxDomainVars contient une liste d'états precedent, soit les variables parents
                    //de la requete en cour ( constituant également une requete lors d'un appel récursif ),
                    //avec une ou des valeurs de domaine assignée. Ce sont des copies
                    //de variables cachées traitées dans la partie max ( correspond à somme ).
                    //qui ont fournis la probabilité maximum pour la partie transition mutlipliée
                    //par la partie récursive, la valeur max ayant été sauvegardée dans la (les) variable.

                    maxParentStates.addAll(rs.maxDomainVars);
                }
            }

            if (!requestTime0.isEmpty()) {

                //pour les variables situées au temps 0 ont obtient directement leur probabilité
                AbstractDouble req0Prob = megaRequest0.getProbability();

                requestValueProbability = requestValueProbability.multiply(req0Prob);

                requestValueMaxProbability = requestValueMaxProbability.multiply(req0Prob);
            }
            //Si la combinaison sur laquelle on travaille actuellement contient
            //plus de variables que celles requete originale
            //mais cependant necessaires pour le calcul on s'interesse
            //ici uniquement à la combinaison de valeurs pour les variables de la requete d'origine
            //sinon il faudrait retrouver cette combinaison parmis plusieurs autres qui la contiendrait
            //EX : P(X1=v,X2=v) = P(X1=v,X2=v,X3=v) + P(X1=v,X2=v,X3=f)
            //pour chaque combinaison de valeur de la requete d'origine on enregistre une probabilité
            //et si la requete a été complété les variables étant les mêmes instances on peut
            //facilement recuperer la ou les valeurs de domaines à partir de la liste d'origine
            //ou megavaribale pour simplifier.

            Domain.DomainValue requestDomainValue = megaRequest.getDomainValue();

            if (forwardMatrix.get(requestDomainValue) == null) {
                //enregistrement de la probabilité pour la ou les valeur courante attribuées
                //à la ou les variables de requete
                //System.out.println("forwardMatrix PUT "+requestDomainValue+" = "+requestValueProbability);
                forwardMatrix.put(requestDomainValue, requestValueProbability);

                maxMatrix.put(requestDomainValue, requestValueMaxProbability);

            } else {
                //enregistrement de la probabilité pour la valeur courante de la requete additionnée à la precedente
                //pour une même combinaison, étant donné que si la requete est complétée par d'autres variables,
                //la combinaisons de valeurs de la requete d'origine peut apparaitre plusieurs fois

                //System.out.println("forwardMatrix ADD AND PUT "+requestDomainValue+" = "+forwardMatrix.get(requestDomainValue)+" + "+requestValueProbability);

                forwardMatrix.put(requestDomainValue, forwardMatrix.get(requestDomainValue).add(requestValueProbability));

                maxMatrix.put(requestDomainValue, maxMatrix.get(requestDomainValue).add(requestValueMaxProbability));
            }

            //pour chaque combinaison de valeur de la requete enregistre
            //la liste des variables et leur valeurs pour celles qui offrent le max.
            mostLikelyPath.put(requestDomainValue, new ArrayList<>(maxParentStates));

            totalDistribution = totalDistribution.add(requestValueProbability);

            //System.out.println("TOTAL : "+totalDistribution+" = "+requestValueProbability);
        }
        //enregistre les totaux pour toutes les combinaisons
        //pour les maximum inutile d'enregistrer les totaux
        //etant donné que normaliser ne change pas le rapport de grandeur entre les valeurs

        //System.out.println("FINAL TOTAL " + totalDistribution);

        forwardMatrix.putTotal(totalDistribution);

        fullMegaRequest.setDomainValue(originalValue);

        this.mostLikelyPath.put(key, mostLikelyPath);

        //normalise le forward
        forwardMatrix.normalize();

        if (saveforward) {

            this.forwardMatrices.put(key, forwardMatrix);

            this.maxMatrices.put(key, maxMatrix);
        }

        return new ForwardMax(forwardMatrix, maxMatrix);
    }

    protected ForwardSumRs forwardHiddenVarSum(List<Variable> actions, Variable obsParentState,
                                               Map<String, Distribution> forwardMatrices, Map<String, Distribution> maxMatrices,
                                               int depth, boolean saveforward) {

        //avant de demarrer la sommation, les variables parent des observations non initialisées doivent être reseter à la fin
        //car si elles conservent leur valeur la recuperation des combinaison echoue pour celle initialisées
        //lors d'un rappel de la sommation pour une combinaison differente dans la procedure appelante

        //Par contre si une variable parent de observation est déja initialisée ce qui peut être le cas
        //si elle fait déja partie de la requete dans la procedure appelante, elle doit rester en l'état
        //(cas spécifique si l'ordre de markov des dependances d'état à état est superieur à 1 ...)

        List<Variable> statesDependencies = new LinkedList<>(obsParentState.getDependencies());
        //si des actions sont parent d'un état ils sont ignorés.
        //pour le cas du PDMPO on calcule le forward étape par étape en initialiser une action et une observation
        //en incrementant le temps de 1 à chaque fois, et pour calculer l'état le forward precedent ne doit
        //porter que sur la ou les variables états
        statesDependencies.removeAll(actions);

        Variable megaHiddenVar = network.encapsulate(statesDependencies);
        //Variable megaHiddenVar = network.encapsulate(obsParentState.getDependencies());

        Domain.DomainValue originalValue = megaHiddenVar.saveDomainValue();

        AbstractDouble hiddenVarMax = this.network.getDoubleFactory().getNew(0.0);

        List<Variable> maxHiddenvars = null;

        AbstractDouble hiddenVarsSum = this.network.getDoubleFactory().getNew(0.0);
        //une variable de la requete peut avoir plusieurs parents il faut donc récuperer les combinaisons de valeurs
        //pour sommer sur chacune d'entre elle.
        //CAS SPECIFIQUES POUR LES CHAINES DE MARKOV DE NIVEAU SUPERIEUR A 1
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

        String key = getDistribSavedKey(statesDependencies);

        //System.out.println("FORWARD SUM DEPENDENCIES KEY "+key);
        //String key = getDistribSavedKey(obsParentState.getDependencies());

        List<Domain.DomainValue> domainValues = megaHiddenVar.getDomainValuesCheckInit();

        //pour chaque combinaison de valeurs
        for (Domain.DomainValue domainValue : domainValues) {

            megaHiddenVar.setDomainValue(domainValue);

            //début de la multiplication avec la valeur fourni par le modele de transition
            AbstractDouble mulTransitionForward = obsParentState.getProbability();
            //on a ici potentiellement une filtrage sur plusieurs variable si plusieurs variables cachées

            Distribution forward = forwardMatrices.get(key);

            Distribution max = maxMatrices.get(key);

            if (forward == null) {

                //System.out.println("!!!!!!! FORWARD NULL RAPPEL RECURSIF !!!!!!!!!!!!");

                ForwardMax forwardMax = this.forward(statesDependencies, actions, key, depth + 1, saveforward, forwardMatrices, maxMatrices);

                //ForwardMax forwardMax = this.forward(obsParentState.getDependencies(), actions, key, depth + 1, saveforward, forwardMatrices);

                max = forwardMax.max;

                forward = forwardMax.forward;

                forwardMatrices.put(key, forward);

                maxMatrices.put(key, max);
            }

            //on recupere la probabilité de la sous sequence la plus probable (max) pouvant mener
            //à la (les) valeur courante de la(les) variable cachée
            // AbstractDouble maxValue = max.get(domainValue);

            AbstractDouble maxValue = max.get(domainValue);

            //System.out.println("maxValue "+maxValue);

            //pour gerer temporairement les max avec le PDMPO ou la distribution est bogus
            if(maxValue == null){

                maxValue = network.getDoubleFactory().getNew(0.0);
            }
            //cette valeur est multipliée par la probabilité obtenu par le modele de transition,
            //soit les variable états enfants avec leur valeurs courantes (les variables de la requete complétée
            // dans la fonction appelante) sachant les variables cachées et leur valeur courante.
            //ce qui pondère la sous sequence la plus probable.

            maxValue = maxValue.multiply(obsParentState.getProbability());

            if (maxValue.compareTo(hiddenVarMax) >= 0) {

                hiddenVarMax = maxValue;
                //sauvegarde l'état des variables cachées pour le maximum
                //les valeurs pouvant encore changer dans la procedure
                List<Variable> copyDependencies = new LinkedList<>();

                // for (Variable dependencie : obsParentState.getDependencies()) {
                for (Variable dependencie : statesDependencies) {

                    copyDependencies.add(dependencie.copyLabelTimeValueDom());
                }

                maxHiddenvars = copyDependencies;
            }


            //recupere le resultat du forward pour la combinaison de valeurs courantes des variables cachées
            AbstractDouble forwardValue = forward.get(domainValue);//.divide(forward.getTotal());
            //on multiplie le resultat du forward avec la partie transition
            //mulTransitionForward = mulTransitionForward.multiply(forwardValue);
            mulTransitionForward = mulTransitionForward.multiply(forwardValue);
            //on additionne pour chaque combinaison de valeur pour les variables cachées
            hiddenVarsSum = hiddenVarsSum.add(mulTransitionForward);

        }

        megaHiddenVar.setDomainValue(originalValue);

        return new ForwardSumRs(hiddenVarsSum, hiddenVarMax, maxHiddenvars, key);
    }


    public List<Variable> computeMostLikelyPath(Variable... request) {

        return computeMostLikelyPath(asList(request));
    }

    public List<Variable> computeMostLikelyPath(List<Variable> requests) {

        Variable megaRequest = network.encapsulate(requests);
        //la sequence d'états la plus probable est calculée à partir d'une liste d'états
        //de 0 à un temps t. la liste des variables et leur ordre doit être le même
        //que lors du premier appel à la methode forward
        //on récupere la clé correspondant aux variables
        // String key = Util.getDistribSavedKey(requests);

        String key = megaRequest.getVarTimeId();

        Distribution maxProbs = this.maxMatrices.get(key);

        Map<Domain.DomainValue, List<Variable>> maxPath = this.mostLikelyPath.get(key);

        Domain.DomainValue maxValue = null;

        AbstractDouble max = network.getDoubleFactory().getNew(0.0);
        //on calcul la combinaison de valeur pour la requete
        //ayant la plus grande probabilité
        for (Domain.DomainValue value : maxProbs.getRowValues()) {

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

        Variable megaMaxVar = network.encapsulate(maxVars);

        //ajoute les variables courantes avec leurs valeurs de domaine
        mostLikelyPath.add(megaMaxVar);
        //récupère la clé correspondant à la liste de variables courantes

        this.loadMostLikelyPath(mostLikelyPath, this.mostLikelyPath.get(megaMaxVar.getVarTimeId()).get(megaMaxVar.getDomainValue()));
    }

    /*------------------------------- SIMPLE FORWARD ----------------------------------*/

    public ForwardMax forward(Variable megaState, Variable megaObservation) {

        return this.forward(megaState, megaObservation, network.getTime(), null, null, false);
    }

    public ForwardMax forward(Variable megaState, Variable megaObservation, Matrix previousForward, Matrix previousMax) {

        return this.forward(megaState, megaObservation, network.getTime(), (Distribution) previousForward, (Distribution) previousMax, false);
    }

    private ForwardMax forward(Variable megaState, Variable megaObservation, int time,
                               Distribution previousForward, Distribution previousMax, boolean saveForward) {

        //valeur originale de la requete
        Domain.DomainValue originalValue = megaState.getDomainValue();
        //distribution pour la requete courante
        Distribution forward = new Distribution(megaState, network.getDoubleFactory());
        //distribution max pour la requete courante
        Distribution forwardMax = new Distribution(megaState, network.getDoubleFactory());
        //meilleurs valeurs precedentes pour chaque valeur de la requete courante
        Map<Domain.DomainValue, Domain.DomainValue> maxPath = new Hashtable<>();
        //distribution de base sans dependances
        if (time == network.getInitTime()) {

            for (Domain.DomainValue value : megaState.getDomainValues()) {

                megaState.setDomainValue(value);

                AbstractDouble prob = megaState.getProbability();

                forward.put(value, prob);

                forwardMax.put(value, prob);
            }
            //version de base déja normalisé (somme des probs de chaque valeur à 100 %)
            //forward.normalize();

            if (saveForward) {

                this.maxMatrices.put(megaState.getVarTimeId(), forwardMax);

                this.forwardMatrices.put(megaState.getVarTimeId(), forward);
            }

            megaState.setDomainValue(originalValue);

            return new ForwardMax(forward, forwardMax);
        }

        //megavariable observations precedentes
        Variable previousMegaObservation = network.getMegaObs(megaObservation, time - 1);
        //megavariable états precedents
        Variable previousMegaState = network.getMegaState(megaState, time - 1);

        for (Domain.DomainValue requestValue : megaState.getDomainValues()) {

            megaState.setDomainValue(requestValue);
            //somme des probabilités de transition multiplié par les probablités des états précédents
            AbstractDouble sum = network.getDoubleFactory().getNew(0.0);
            //max des probablités de transition multipliés par la probabilité du chemin max menant à l'état precedent
            AbstractDouble max = network.getDoubleFactory().getNew(0.0);

            Domain.DomainValue maxPreviousValue = null;
            //pour chaque variables cachés (les parents de la requete courante)
            //dans cette procedure l'ensemble des états à chaque coupe temporelle
            for (Domain.DomainValue hiddenValue : previousMegaState.getDomainValues()) {
                //initialise les variables cachées
                previousMegaState.setDomainValue(hiddenValue);
                //appel recursif la premiere pour calculer la distribution forward et max
                //sur les variables cachées, puis sauvegarde
                if (previousForward == null) {

                    ForwardMax rs = forward(previousMegaState, previousMegaObservation,
                            time - 1, previousForward, previousMax, saveForward);

                    previousForward = rs.forward;

                    previousMax = rs.max;
                }
                //probabilité de transition de la requete
                AbstractDouble requestProb = megaState.getProbability();
                //transition multiplié par le message avant precedent ajouté à la somme
                sum = sum.add(requestProb.multiply(previousForward.get(hiddenValue)));
                //transition multiplié par le max precedent
                AbstractDouble computeMax = requestProb.multiply(previousMax.get(hiddenValue));
                //calcul de la valeur precedente maximum pondérée par les probabilités de transition
                if (computeMax.compareTo(max) >= 0) {

                    max = computeMax;

                    maxPreviousValue = hiddenValue;
                }
            }
            //probabilité d'observation par rapport à la valeur de requete courante
            AbstractDouble observationProb = megaObservation.getProbability();
            //enregistrement des valeur de distribution forward et max
            forward.put(requestValue, observationProb.multiply(sum));

            forwardMax.put(requestValue, observationProb.multiply(max));
            //enregistrement pour la valeur de requete courante de la valeur de
            //requete precedent offrant la probabilité maximum pour calcul du chemin à postériori
            maxPath.put(requestValue, maxPreviousValue);
        }

        //reset de la valeur de requete
        if (originalValue != null) {

            megaState.setDomainValue(originalValue);
        }

        //enregistrement des chemins les plus probables pour la requete courante
        this.mostLikelyPathFull.put(megaState.getVarTimeId(), maxPath);

        forward.normalize();

        if (saveForward) {

            this.forwardMatrices.put(megaState.getVarTimeId(), forward);
        }

        return new ForwardMax(forward, forwardMax);
    }

    public List mostLikelyPath(Variable megaRequest, Matrix lastMax, int time) {

        // System.out.println("MOST LIKELY PATH COMPUTE");

        Distribution lastMaxDistrib = (Distribution) lastMax;
        //calculer la valeur max pour la derniere requete
        AbstractDouble max = network.getDoubleFactory().getNew(0.0);

        Domain.DomainValue maxValue = null;

        for (Domain.DomainValue value : lastMaxDistrib.getRowValues()) {

            AbstractDouble valueProb = lastMaxDistrib.get(value);

            if (valueProb.compareTo(max) >= 0) {

                max = valueProb;

                maxValue = value;
            }
        }

        // System.out.println("MAX POUR " + megaRequest.getVarTimeId() + " " + maxValue);

        //ajoute la valeur dans la liste
        LinkedList<Map.Entry<Integer, Domain.DomainValue>> mostLikelyValues = new LinkedList();

        mostLikelyValues.addFirst(new AbstractMap.SimpleEntry<>(time, maxValue));
        //récupere les chemins vers les valeurs precedentes
        Map<Domain.DomainValue, Domain.DomainValue> maxPath = this.mostLikelyPathFull.get(megaRequest.getVarTimeId());
        //récupère la suite du chemin tant qu'il y en a
        //et tant que le temps est supérieur à 1, le premier forward à lieu au temps 1
        //qui est le premier temps ou l'on fait une estimation.
        while (maxPath != null && time > 1) {
            //temps precedent
            time--;

            // System.out.println("TIME " + (time));

            //la valeur maximum précédente
            maxValue = maxPath.get(maxValue);
            //ajout de la valeur dans al liste max
            mostLikelyValues.addFirst(new AbstractMap.SimpleEntry<>(time, maxValue));
            //recharge la megaRequete
            megaRequest = network.getMegaState(megaRequest, time);
            //et les chemins precedents
            maxPath = this.mostLikelyPathFull.get(megaRequest.getVarTimeId());

            // System.out.println("MAX POUR " + megaRequest + " " + maxValue);
        }

        return mostLikelyValues;
    }

    /*------------------------------- VIEW ----------------------------------*/

    public void showForward() {

        for (Map.Entry<String, Distribution> entry : this.forwardMatrices.entrySet()) {

            System.out.println(entry.getKey() + entry.getValue());
        }
    }


    public void showMax() {

        for (Map.Entry<String, Distribution> entry : this.maxMatrices.entrySet()) {

            System.out.println(entry.getKey() + entry.getValue());
        }
    }


    /*------------------------------- GETTER SETTER ----------------------------------*/

    public Map<String, Distribution> getForwardMatrices() {
        return forwardMatrices;
    }

    public Map<String, Distribution> getMaxMatrices() {
        return maxMatrices;
    }

    public Map<String, Map<Domain.DomainValue, List<Variable>>> getMostLikelyPath() {
        return mostLikelyPath;
    }


    /*------------------------------- SOUS CLASSES ----------------------------------*/


    public class ForwardMax {

        Distribution forward, max;

        public ForwardMax(Distribution forwardMatrix, Distribution maxMatrix) {

            this.forward = forwardMatrix;

            this.max = maxMatrix;
        }

        public Distribution getForward() {
            return forward;
        }

        public Distribution getMax() {
            return max;
        }
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

}
