package inference.dynamic;

import domain.Domain;
import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.MegaVariable;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.*;

import static inference.dynamic.Util.*;
import static network.BayesianNetwork.domainValuesCombinations;

public class Backward {

    protected static boolean backwardLogCompute = false, backwardLogDetails = false;

    protected Map<String, Map<Domain.DomainValue, AbstractDouble>> backwardDistribSaved = new Hashtable<>();

    protected Map<String, Map<Domain.DomainValue, AbstractDouble>> backwardFullDistribSaved = new Hashtable<>();

    protected DynamicBayesianNetwork network;

    public Backward(DynamicBayesianNetwork network) {

        this.network = network;
    }

    /*------------------- BACKWARD --------------------*/

    public void backward(List<Variable> requests, String key, int depth, int timeEnd) {

        String ident = getIdent(depth);

        if (backwardLogDetails) {

            System.out.println(ident + "REQUEST " + requests);
        }

        Variable megaRequest = requests.size() == 1 ? requests.get(0) : MegaVariable.encapsulate(requests);

        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();

        //atteind une requete située à la derniere phase temporelle si plusieurs elles doivent être au même temps
        if (requests.get(0).getTime() == timeEnd) {

            for (Domain.DomainValue domainValue : megaRequest.getDomainValues()) {

                distribution.put(domainValue, network.getDoubleFactory().getNew(1.0));
            }

            distribution.put(totalDomainValues, network.getDoubleFactory().getNew(1.0));

            this.backwardDistribSaved.put(key, distribution);

            return;
        }

        //récuperation des observations de la variable au temps suivant
        //et ajout des eventuelles variables manquante à la requete
        Set<String> containState = new HashSet<>(), containObs = new HashSet<>();

        List<Variable> fullRequest = new LinkedList<>(), nextObservations = new LinkedList<>();

        for (Variable request : requests) {

            //ajout des requetes dans la liste complétée
            if (!containState.contains(request.getVarTimeId())) {

                containState.add(request.getVarTimeId());

                fullRequest.add(request);

                //récupération de la variable au temps suivant
                Variable nextRequest = this.network.getVariable(request.getTime() + 1, request);
                //si variable en temps courant du mmc aucune ne suit
                if (nextRequest != null) {

                    for (Variable nextObservation : nextRequest.getObservations()) {
                        //ignore les observations déjas enregistrées
                        if (!containObs.contains(nextObservation.getVarTimeId())) {

                            containObs.add(nextObservation.getVarTimeId());

                            nextObservations.add(nextObservation);
                            //maintenant chaque observation sera calculé en fonction de ses parents dans partie 1 de la somme
                            for (Variable observationDep : nextObservation.getDependencies()) {
                                //et le parent de l'observation sera calculé en fonction des siens en partie 3
                                //cependant certain pourrait ne pas faire partie de la requete originale
                                //et il faut donc les rajouter
                                for (Variable missingRequest : observationDep.getDependencies()) {

                                    if (!containState.contains(missingRequest.getVarTimeId())) {

                                        containState.add(missingRequest.getVarTimeId());

                                        fullRequest.add(missingRequest);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Variable megaFullRequest = fullRequest.size() == 1 ? fullRequest.get(0) : MegaVariable.encapsulate(fullRequest);

        //Variable megaObservation = nextObservations.size() == 1 ? nextObservations.get(0) : MegaVariable.encapsulate(nextObservations);

        //sauvegarde des valeurs originales de la requete
        Domain.DomainValue originalValue = megaFullRequest.getDomainValue();

        if (backwardLogDetails) {

            System.out.println(ident + "FULL REQUEST " + fullRequest);
        }

        AbstractDouble totalRequest = network.getDoubleFactory().getNew(0.0);
        //pour combinaison de valeur de la requete
        for (Domain.DomainValue domainValue : megaFullRequest.getDomainValues()) {

            megaFullRequest.setDomainValue(domainValue);

            if (backwardLogDetails) {

                System.out.println(ident + "FULL REQUEST INIT COMBINATION : " + fullRequest);
            }

            //multiplier le resultat pour chaque observation
            AbstractDouble multiplyObservations = network.getDoubleFactory().getNew(1.0);

            for (Variable nextObservation : nextObservations) {

                multiplyObservations = multiplyObservations.multiply(backwardSum(nextObservation, depth + 1, timeEnd));
            }

            Domain.DomainValue requestDomainValue = megaRequest.getDomainValue();

            if (!distribution.containsKey(requestDomainValue)) {
                //enregistrement de la probabilité pour la valeur courante de la requete
                distribution.put(requestDomainValue, multiplyObservations);

            } else {
                //enregistrement de la probabilité pour la valeur courante de la requete additionné à la precedente
                //pour une même combinaison
                distribution.put(requestDomainValue, distribution.get(requestDomainValue).add(multiplyObservations));
            }

            totalRequest = totalRequest.add(multiplyObservations);
        }

        distribution.put(totalDomainValues, totalRequest);

        megaFullRequest.setDomainValue(originalValue);

        if (backwardLogDetails) {

            System.out.println(ident + "KEY " + key);

            System.out.println(ident + "DISTRIB : " + distribution);
        }

        this.backwardDistribSaved.put(key, distribution);
    }

    protected AbstractDouble backwardSum(Variable nextObservation, int depth, int timeEnd) {

        String ident = null;

        if (backwardLogDetails) {

            ident = getIdent(depth);
        }

        AbstractDouble sum = network.getDoubleFactory().getNew(0.0);

        Variable megaHidenVar = nextObservation.getDependencies().size() == 1 ?
                nextObservation.getDependencies().get(0) :
                MegaVariable.encapsulate(nextObservation.getDependencies());

        Domain.DomainValue megaHidenVarOriginalValue = megaHidenVar.getDomainValue();

        String key = getDistribSavedKey(nextObservation.getDependencies());

        if (backwardLogDetails) {

            System.out.println(ident + "SUM ON " + nextObservation.getDependencies());

            System.out.println(ident + "KEY  " + key);
        }

        for (Domain.DomainValue domainValue : megaHidenVar.getDomainValues()) {

            megaHidenVar.setDomainValue(domainValue);

            AbstractDouble multiplyUnderSum = network.getDoubleFactory().getNew(1.0);

            if (backwardLogDetails)
                System.out.println(ident + "SUM DEPENDENCY " + nextObservation.getDependencies());

            if (backwardLogCompute) System.out.print(nextObservation.getProbabilityForCurrentValue());

            multiplyUnderSum = multiplyUnderSum.multiply(nextObservation.getProbabilityForCurrentValue());

            Map<Domain.DomainValue, AbstractDouble> hiddenVarsDistribution = this.backwardDistribSaved.get(key);

            if (hiddenVarsDistribution == null) {

                this.backward(nextObservation.getDependencies(), key, depth + 1, timeEnd);

                hiddenVarsDistribution = this.backwardDistribSaved.get(key);
            }

            Domain.DomainValue megaHiddenVarValue = megaHidenVar.getDomainValue();

            if (backwardLogDetails) {
                System.out.println(ident + "DISTRIB : " + hiddenVarsDistribution);
                System.out.println(ident + "COMBI : " + megaHiddenVarValue);
                System.out.println(ident + "RESULT : " + hiddenVarsDistribution.get(megaHiddenVarValue));
            }

            AbstractDouble combinaisonProb = hiddenVarsDistribution.get(megaHiddenVarValue);

            if (backwardLogCompute)
                System.out.print(" * " + combinaisonProb.divide(hiddenVarsDistribution.get(totalDomainValues)));

            multiplyUnderSum = multiplyUnderSum.multiply(combinaisonProb.divide(hiddenVarsDistribution.get(totalDomainValues)));

            multiplyUnderSum = multiplyUnderSum.multiply(megaHidenVar.getProbabilityForCurrentValue());

            if (backwardLogCompute || backwardLogDetails) {

                for (Variable hiddenVar : nextObservation.getDependencies()) {

                    if (backwardLogCompute) System.out.print(" * " + hiddenVar.getProbabilityForCurrentValue());

                    if (backwardLogDetails)
                        System.out.println(ident + "HIDDEN VAR PROB  P(" + hiddenVar + "|" + hiddenVar.getDependencies() + ") = " + hiddenVar.getProbabilityForCurrentValue());
                }

            }

            if (backwardLogCompute) System.out.print(" + ");

            sum = sum.add(multiplyUnderSum);
        }

        megaHidenVar.setDomainValue(megaHidenVarOriginalValue);

        return sum;
    }

    /**
     * Tentative d'implementation du backward pour gerer les chaines de markov d'order superieur à 1
     */
    public void backward(List<Variable> requests, String key, int depth, int timeEnd, int markovOrder) {

        String ident = getIdent(depth);

        if (backwardLogDetails)
            System.out.println(ident + "REQUEST " + requests);

        Map<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>(),
                fullDistribution = new Hashtable<>();

        //atteind une requete située à la derniere phase temporelle si plusieurs elles doivent être au même temps
        if (requests.get(0).getTime() == timeEnd) {

            List<List<Domain.DomainValue>> domainValuesLists = BayesianNetwork.domainValuesCombinations(requests);

            for (List<Domain.DomainValue> domainValues : domainValuesLists) {

                Domain.DomainValue domainValue = new Domain.DomainValue(domainValues);

                distribution.put(domainValue, network.getDoubleFactory().getNew(1.0));

                fullDistribution.put(domainValue, network.getDoubleFactory().getNew(1.0));
            }

            distribution.put(totalDomainValues, network.getDoubleFactory().getNew(1.0));

            fullDistribution.put(totalDomainValues, network.getDoubleFactory().getNew(1.0));

            this.backwardDistribSaved.put(key, distribution);

            this.backwardFullDistribSaved.put(key, fullDistribution);

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
         * la requete APPEL_1 est s(1) , l'observation suivante o(2), l'état parent de l'observation qui est la variable cachée
         * est s(2) a deux parents s(1),s(0) par consequent on complete la requete par s(0)
         * on travaille donc sur des combinaisons de valeurs pour s(1),s(0) en dehors de la somme,
         * puis dans la 3eme partie de la somme (qui se fait sur les valeurs prises par s(2))
         * s(2) est calculée par rapport à une assignation de valeur fixes à s(1),s(0).
         * Maintenant la 2eme partie récursive ou s(2) devient la requete APPEL_2, on recommence le processus
         * et cette fois ci la requete est complétée par s(1) étant donné que s(3) en dépend également.
         * Et même si dans APPEL_1 s(1) à obtenu une valeur si on veut eviter de refaire toute le calcul à chaque appel
         * récursif on doit le faire sur toutes les valeurs de s(1).
         * on se retrouve donc avec une distribution sur s(1) et s(2). Si s(1) est necessaire pour calculer s(3)
         * lorsqu'on revient à la boucle sur les valeurs de s(2) dans APPEL_1 à la fin de l'appel recursif de APPEL_2
         * le point délicat et que à ce moment s(2) est aussi calculée
         * par rapport à s(1) et que s(1) à déja une valeur assignée. Par consequent il faut pouvoir
         * recuperer la bonne valeur pour s(2) retournée par APPEL_2 en fonction de la valeur de s(1) courante
         *
         * pour une chaine d'ordre x on s'interesse donc à la variable s et à la même variable s sur les temps précédents
         * sur une profondeur x - 1. Pour une chaine de markov 1 ca donnerait uniquement s(2), d'ordre 2, s(2) et s(1)
         *
         *
         * */

        //récuperation des observations de la variable au temps suivant

        Set<String> containState = new HashSet<>(), containObs = new HashSet<>();

        List<Variable> fullRequest = new LinkedList<>(), nextObservations = new LinkedList<>();

        for (Variable request : requests) {

            //ajout des requetes dans la liste complétée
            if (!containState.contains(request.getVarTimeId())) {

                containState.add(request.getVarTimeId());

                fullRequest.add(request);

                //récupération de la variable au temps suivant
                //faut il recuperer les variables deux temps en avant ?
                Variable nextRequest = this.network.getVariable(request.getTime() + 1, request);
                //si variable en temps t aucune ne suit
                if (nextRequest != null) {

                    for (Variable nextObservation : nextRequest.getObservations()) {
                        //ignore les observation déja enregistrées
                        if (!containObs.contains(nextObservation.getVarTimeId())) {

                            containObs.add(nextObservation.getVarTimeId());

                            nextObservations.add(nextObservation);
                            //maintenant chaque observation sera calculé en fonction de ses parents dans partie 1 de la somme
                            for (Variable observationDep : nextObservation.getDependencies()) {
                                //et le parent de l'observation sera calculé en fonction des siens en partie 3
                                //cependant certain pourrait ne pas faire partie de la requete originale
                                //exemple pour une chaine de markov d'ordre 2
                                //une requete sur s(1), on prend l'observation suivante o(2)
                                //o(2) à pour parent s(2) qui depend de s(1) mais aussi de s(0)
                                //donc la partie 3 à besoin d'assigner une valeur à s(0) et s(1) pour calculer s(2)
                                //qui doit completer la requete même si dans la distribution finale
                                //on ne prend en compte que s(1) pour ensuite faire la multiplication
                                //avec le forward qui est une distribution sur s(1) également
                                for (Variable missingRequest : observationDep.getDependencies()) {

                                    if (!containState.contains(missingRequest.getVarTimeId())) {

                                        containState.add(missingRequest.getVarTimeId());

                                        fullRequest.add(missingRequest);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //trie des variables par temps puis par label
        Collections.sort(fullRequest, comparatorVarTimeLabel);

        String fullKey = getDistribSavedKey(fullRequest);

        List<Domain.DomainValue> originalValues = new LinkedList<>();

        for (Variable request : fullRequest) {

            originalValues.add(request.getDomainValue());
        }

        if (backwardLogDetails)
            System.out.println(ident + "FULL REQUEST " + fullRequest);

        List<List<Domain.DomainValue>> domainValuesLists = BayesianNetwork.domainValuesCombinations(fullRequest);

        AbstractDouble totalRequest = network.getDoubleFactory().getNew(0.0);
        //pour combinaison de valeur de la requete
        for (List<Domain.DomainValue> fullRequestValue : domainValuesLists) {

            Iterator<Variable> requestsIterator = fullRequest.iterator();

            for (Domain.DomainValue domainValue : fullRequestValue) {

                requestsIterator.next().setDomainValue(domainValue);
            }

            if (backwardLogDetails)
                System.out.println(ident + "FULL REQUEST INIT COMBINATION " + fullKey + " : " + fullRequest);

            //multiplier le resultat pour chaque observation
            AbstractDouble multiplyObservations = network.getDoubleFactory().getNew(1.0);

            for (Variable nextObservation : nextObservations) {

                multiplyObservations = multiplyObservations.multiply(backwardSum(nextObservation, depth + 1, timeEnd, markovOrder));
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

            fullDistribution.put(new Domain.DomainValue(fullRequestValue), multiplyObservations);

            totalRequest = totalRequest.add(multiplyObservations);
        }

        distribution.put(totalDomainValues, totalRequest);

        fullDistribution.put(totalDomainValues, totalRequest);

        Iterator<Variable> requestIterator = fullRequest.iterator();
        //restaure les valeurs originales
        for (Domain.DomainValue domainValue : originalValues) {

            requestIterator.next().setDomainValue(domainValue);
        }

        if (backwardLogDetails) {
            System.out.println(ident + "KEY " + key);

            System.out.println(ident + "FULL KEY " + fullKey);

            System.out.println(ident + "DISTRIB : " + distribution);

            System.out.println(ident + "FULL DISTRIB : " + fullDistribution);
        }
        this.backwardDistribSaved.put(key, distribution);

        this.backwardFullDistribSaved.put(fullKey, fullDistribution);
    }

    protected AbstractDouble backwardSum(Variable nextObservation, int depth, int timeEnd, int markovOrder) {

        String ident = getIdent(depth);

        AbstractDouble sum = network.getDoubleFactory().getNew(0.0);

        List<List<Domain.DomainValue>> hiddenVarsValues = BayesianNetwork.domainValuesCombinations(nextObservation.getDependencies());

        String fullKey = "";

        //récuperer tout les parents de l'observation courante soit
        //normalement uniquement les variables états situées à la même coupe temporelle
        //on commence par ajouter celles là
        List<Variable> keyVars = new LinkedList<>();

        keyVars.addAll(nextObservation.getDependencies());
        //completer les variables dont la valeur on une importance
        //pour le calcul. Pour une variable en temps timeEnd dont la distribution vaut 1
        //c'est inutile
        if (markovOrder > 1 && keyVars.get(0).getTime() < timeEnd) {

            for (Variable nextDep : nextObservation.getDependencies()) {
                // System.out.println(ident+"NEXT DEP :  "+nextDep);
                //on recupere les variables de même label situées à des temps precedents
                //en fonction de la profondeur de la chaine de markov
                //qui pourrait bien être spécifique à certaines variables
                //plutot que général dans ce cas l'information serait à récuperer dans la variable
                //plutot que comme parametre de la procedure...
                //On recupere les markovOrder - 1 variables precedentes
                for (int d = markovOrder; d > 1 && nextDep.getTime() > 0; d--) {
                    //System.out.println(ident+"Previous time state : "+this.getVariable(nextDep.getTime() - 1, nextDep));
                    keyVars.add(this.network.getVariable(nextDep.getTime() - 1, nextDep));
                }
            }
        }

        Collections.sort(keyVars, comparatorVarTimeLabel);

        fullKey = getDistribSavedKey(keyVars);

        String simpleKey = getDistribSavedKey(nextObservation.getDependencies());
        if (backwardLogDetails) {
            System.out.println(ident + "SUM ON " + nextObservation.getDependencies());
            System.out.println(ident + "SUM ON FULL" + keyVars);
            System.out.println(ident + "KEY  " + simpleKey);
            System.out.println(ident + "FULL KEY " + fullKey);
        }
        for (List<Domain.DomainValue> domainValues : hiddenVarsValues) {

            AbstractDouble multiplyUnderSum = network.getDoubleFactory().getNew(1.0);

            Iterator<Variable> obsDependenciesIterator = nextObservation.getDependencies().iterator();

            for (Domain.DomainValue depValue : domainValues) {

                obsDependenciesIterator.next().setDomainValue(depValue);
            }

            if (backwardLogDetails)
                System.out.println(ident + "SUM DEPENDENCY " + nextObservation.getDependencies());


            if (backwardLogCompute) System.out.print(nextObservation.getProbabilityForCurrentValue());

            multiplyUnderSum = multiplyUnderSum.multiply(nextObservation.getProbabilityForCurrentValue());

            Map<Domain.DomainValue, AbstractDouble> hiddenVarsFullDistribution = this.backwardFullDistribSaved.get(fullKey);

            /* ici il faut travailler sur la distribution sur le parent de l'observation complétée
             * recuperer en fonction de l'ordre les predecesseurs de ce parent
             * et avec la clé comprenant les labels et temps recuperer la bonne valeur
             * qui vient d'être calculée
             * pour cela il faudrait enregistrer l'ordre des dependances dans la variable
             * mais aussi trier les variables de la requete complete par temps et par label
             * */

            if (hiddenVarsFullDistribution == null) {

                this.backward(nextObservation.getDependencies(), simpleKey, depth + 1, timeEnd, markovOrder);

                hiddenVarsFullDistribution = this.backwardFullDistribSaved.get(fullKey);
            }

            List<Domain.DomainValue> fullDistribValues = new LinkedList<>();

            for (Variable keyVar : keyVars) {

                fullDistribValues.add(keyVar.getDomainValue());
            }
            if (backwardLogDetails) {
                System.out.println(ident + "DISTRIB : " + hiddenVarsFullDistribution);
                System.out.println(ident + "COMBI : " + new Domain.DomainValue(fullDistribValues));
                System.out.println(ident + "RESULT : " + hiddenVarsFullDistribution.get(new Domain.DomainValue(fullDistribValues)));
            }
            AbstractDouble combinaisonProb = hiddenVarsFullDistribution.get(new Domain.DomainValue(fullDistribValues));

            if (backwardLogCompute)
                System.out.print(" * " + combinaisonProb.divide(hiddenVarsFullDistribution.get(totalDomainValues)));

            multiplyUnderSum = multiplyUnderSum.multiply(combinaisonProb.divide(hiddenVarsFullDistribution.get(totalDomainValues)));

            for (Variable hiddenVar : nextObservation.getDependencies()) {

                if (backwardLogCompute) System.out.print(" * " + hiddenVar.getProbabilityForCurrentValue());

                multiplyUnderSum = multiplyUnderSum.multiply(hiddenVar.getProbabilityForCurrentValue());
                if (backwardLogDetails)
                    System.out.println(ident + "HIDDEN VAR PROB  P(" + hiddenVar + "|" + hiddenVar.getDependencies() + ") = " + hiddenVar.getProbabilityForCurrentValue());
            }

            if (backwardLogCompute) System.out.print(" + ");

            sum = sum.add(multiplyUnderSum);
        }

        return sum;
    }


    public void showBackward() {

        showDistributions("BACKWARD", this.backwardDistribSaved);
    }


    public void showBackwardDistributions() {

        System.out.println("===========BACKWARD============");

        showDynamicDistributions(this.backwardDistribSaved);
    }

    public void showFullBackwardDistributions() {

        System.out.println("===========FULL BACKWARD============");

        showDynamicDistributions(this.backwardFullDistribSaved);
    }


}
