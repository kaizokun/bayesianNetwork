package inference.dynamic;

import domain.Domain;
import domain.data.AbstractDouble;
import math.Distribution;
import network.MegaVariable;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.*;

import static inference.dynamic.Util.*;

public class Backward {

    protected Map<String, Distribution> backwardMatrices = new Hashtable<>();

    protected DynamicBayesianNetwork network;

    public Backward(DynamicBayesianNetwork network) {

        this.network = network;
    }

    /*------------------- BACKWARD --------------------*/

    public Distribution backward(List<Variable> requests, String key, int depth, int timeEnd, boolean saveDistrib) {

        Variable megaRequest = network.encapsulate(requests);

        Distribution distribution = new Distribution(megaRequest, network.getDoubleFactory());

        //atteind une requete située à la derniere phase temporelle si plusieurs elles doivent être au même temps
        if (requests.get(0).getTime() == timeEnd) {

            for (Domain.DomainValue domainValue : megaRequest.getDomainValues()) {

                distribution.put(domainValue, network.getDoubleFactory().getNew(1.0));
            }

            distribution.putTotal(network.getDoubleFactory().getNew(1.0));

            if (saveDistrib) {

                this.backwardMatrices.put(key, distribution);
            }

            return distribution;
        }

        Map<String, Distribution> backwardDistribSaved = new Hashtable<>();
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

        Variable megaFullRequest = network.encapsulate(fullRequest);
        //Variable megaObservation = nextObservations.size() == 1 ? nextObservations.get(0) : network.encapsulate(nextObservations);

        //sauvegarde des valeurs originales de la requete
        Domain.DomainValue originalValue = megaFullRequest.getDomainValue();
        //AbstractDouble totalRequest = network.getDoubleFactory().getNew(0.0);
        //pour combinaison de valeur de la requete
        for (Domain.DomainValue domainValue : megaFullRequest.getDomainValues()) {

            megaFullRequest.setDomainValue(domainValue);
            //multiplier le resultat pour chaque observation
            AbstractDouble multiplyObservations = network.getDoubleFactory().getNew(1.0);

            for (Variable nextObservation : nextObservations) {

                multiplyObservations = multiplyObservations.multiply(
                        backwardSum(nextObservation, backwardDistribSaved, depth + 1, timeEnd, saveDistrib));
            }

            Domain.DomainValue requestDomainValue = megaRequest.getDomainValue();

            if (distribution.get(requestDomainValue) == null) {
                //enregistrement de la probabilité pour la valeur courante de la requete
                distribution.put(requestDomainValue, multiplyObservations);

            } else {
                //enregistrement de la probabilité pour la valeur courante de la requete additionné à la precedente
                //pour une même combinaison
                distribution.put(requestDomainValue, distribution.get(requestDomainValue).add(multiplyObservations));
            }
        }

        megaFullRequest.setDomainValue(originalValue);

        if (saveDistrib) {

            this.backwardMatrices.put(key, distribution);
        }

        return distribution;
    }

    protected AbstractDouble backwardSum(Variable nextObservation,
                                         Map<String, Distribution> backwardSaved,
                                         int depth, int timeEnd, boolean saveDistrib) {

        AbstractDouble sum = network.getDoubleFactory().getNew(0.0);

        Variable megaHidenVar = network.encapsulate(nextObservation.getDependencies());

        Domain.DomainValue megaHidenVarOriginalValue = megaHidenVar.getDomainValue();

        String key = getDistribSavedKey(nextObservation.getDependencies());

        for (Domain.DomainValue domainValue : megaHidenVar.getDomainValues()) {

            megaHidenVar.setDomainValue(domainValue);

            AbstractDouble multiplyUnderSum = network.getDoubleFactory().getNew(1.0);

            multiplyUnderSum = multiplyUnderSum.multiply(nextObservation.getProbability());

            Distribution hiddenVarsDistribution = backwardSaved.get(key);

            if (hiddenVarsDistribution == null) {

                Distribution backward = this.backward(nextObservation.getDependencies(), key, depth + 1, timeEnd, saveDistrib);

                hiddenVarsDistribution = backward;

                backwardSaved.put(key, backward);
            }

            Domain.DomainValue megaHiddenVarValue = megaHidenVar.getDomainValue();

            AbstractDouble combinaisonProb = hiddenVarsDistribution.get(megaHiddenVarValue);

            multiplyUnderSum = multiplyUnderSum.multiply(combinaisonProb.divide(hiddenVarsDistribution.getTotal()));

            multiplyUnderSum = multiplyUnderSum.multiply(megaHidenVar.getProbability());

            sum = sum.add(multiplyUnderSum);
        }

        megaHidenVar.setDomainValue(megaHidenVarOriginalValue);

        return sum;
    }

    /**
     * Tentative d'implementation du backward pour gerer les chaines de markov d'order superieur à 1
     */
    public BackwardRs backward(List<Variable> requests, String key, int depth, int timeEnd, int markovOrder, boolean saveDistrib) {

        Variable megaRequest = network.encapsulate(requests);

        Distribution distribution = new Distribution(megaRequest, network.getDoubleFactory()),
                fullDistribution = new Distribution(megaRequest, network.getDoubleFactory());

        //atteind une requete située à la derniere phase temporelle si plusieurs elles doivent être au même temps
        if (requests.get(0).getTime() == timeEnd) {

            for (Domain.DomainValue domainValue : megaRequest.getDomainValues()) {

                distribution.put(domainValue, network.getDoubleFactory().getNew(1.0));

                fullDistribution.put(domainValue, network.getDoubleFactory().getNew(1.0));
            }

            distribution.putTotal(network.getDoubleFactory().getNew(1.0));

            fullDistribution.putTotal(network.getDoubleFactory().getNew(1.0));

            if (saveDistrib) {

                this.backwardMatrices.put(key, distribution);
            }

            return new BackwardRs(distribution, fullDistribution);
        }

        //récuperation des observations de la variable au temps suivant
        // Chaine de markov d'ordre 2
        //
        //               ________________
        //    __________|_____           |
        //   |          |     |          |
        //   |          |     V          V
        // (s(0))-->(s(1))-->(s(2))-->(s(3))
        //             |        |        |
        //             V        V        V
        //          (o(1))   (o(2))   (o(3))
        //
        // le calcul est un peu plus delicat dans ce cas, par exemple :
        //
        // on à 4 états s(0) , s(1)|s(0) , s(2)|s(1),s(0) , s(3)|s(2),s(1)
        // 3 observations o(1)|s(1)  o(2)|s(2) o(3)|s(3)
        //
        // la requete APPEL_1 est s(1) , l'observation suivante o(2), l'état parent de l'observation qui est la variable cachée
        // est s(2) a deux parents s(1),s(0) par consequent on complete la requete par s(0)
        // on travaille donc sur des combinaisons de valeurs pour s(1),s(0) en dehors de la somme,
        // puis dans la 3eme partie de la somme (qui se fait sur les valeurs prises par s(2))
        // s(2) est calculée par rapport à une assignation de valeur fixes à s(1),s(0).
        // Maintenant la 2eme partie récursive ou s(2) devient la requete APPEL_2, on recommence le processus
        // et cette fois ci la requete est complétée par s(1) étant donné que s(3) en dépend également.
        // Et même si dans APPEL_1 s(1) à obtenu une valeur si on veut eviter de refaire toute le calcul à chaque appel
        // récursif on doit le faire sur toutes les valeurs de s(1).
        // on se retrouve donc avec une distribution sur s(1) et s(2). Si s(1) est necessaire pour calculer s(3)
        // lorsqu'on revient à la boucle sur les valeurs de s(2) dans APPEL_1 à la fin de l'appel recursif de APPEL_2
        // le point délicat et que à ce moment s(2) est aussi calculée
        // par rapport à s(1) et que s(1) à déja une valeur assignée. Par consequent il faut pouvoir
        // recuperer la bonne valeur pour s(2) retournée par APPEL_2 en fonction de la valeur de s(1) courante
        //
        // pour une chaine d'ordre x on s'interesse donc à la variable s et à la même variable s sur les temps précédents
        // sur une profondeur x - 1. Pour une chaine de markov 1 ca donnerait uniquement s(2), d'ordre 2, s(2) et s(1)

        Map<String, Distribution> backwardFullDistribSaved = new Hashtable<>();

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

        Variable megaFullRequest = network.encapsulate(fullRequest);

        fullDistribution = new Distribution(megaFullRequest, network.getDoubleFactory());

        Domain.DomainValue originalValue = megaFullRequest.saveDomainValue();

        //AbstractDouble totalRequest = network.getDoubleFactory().getNew(0.0);
        //pour combinaison de valeur de la requete
        for (Domain.DomainValue fullRequestValue : megaFullRequest.getDomainValues()) {

            megaFullRequest.setDomainValue(fullRequestValue);

            //multiplier le resultat pour chaque observation
            AbstractDouble multiplyObservations = network.getDoubleFactory().getNew(1.0);

            for (Variable nextObservation : nextObservations) {

                multiplyObservations = multiplyObservations.multiply(
                        backwardSum(nextObservation, backwardFullDistribSaved, depth + 1, timeEnd, markovOrder, saveDistrib));
            }

            Domain.DomainValue requestValue = megaRequest.getDomainValue();

            if (distribution.get(requestValue) == null) {
                //enregistrement de la probabilité pour la valeur courante de la requete
                distribution.put(requestValue, multiplyObservations);

            } else {
                //enregistrement de la probabilité pour la valeur courante de la requete additionné à la precedente
                //pour une même combinaison
                distribution.put(requestValue, distribution.get(requestValue).add(multiplyObservations));
            }

            fullDistribution.put(fullRequestValue, multiplyObservations);

        }

        megaFullRequest.setDomainValue(originalValue);

        if (saveDistrib) {

            this.backwardMatrices.put(key, distribution);
        }

        return new BackwardRs(distribution, fullDistribution);
    }

    protected AbstractDouble backwardSum(Variable nextObservation,
                                         Map<String, Distribution> backwardFullDistribSaved,
                                         int depth, int timeEnd, int markovOrder, boolean distribSave) {

        AbstractDouble sum = network.getDoubleFactory().getNew(0.0);

        Variable megaHiddenVar = network.encapsulate(nextObservation.getDependencies());

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

                //on recupere les variables de même label situées à des temps precedents
                //en fonction de la profondeur de la chaine de markov
                //qui pourrait bien être spécifique à certaines variables
                //plutot que général dans ce cas l'information serait à récuperer dans la variable
                //plutot que comme parametre de la procedure...
                //On recupere les markovOrder - 1 variables precedentes
                for (int d = markovOrder; d > 1 && nextDep.getTime() > 0; d--) {

                    keyVars.add(this.network.getVariable(nextDep.getTime() - 1, nextDep));
                }
            }
        }

        Collections.sort(keyVars, comparatorVarTimeLabel);

        Variable megaKeyVar = network.encapsulate(keyVars);

        String fullKey = getDistribSavedKey(keyVars);

        String simpleKey = getDistribSavedKey(nextObservation.getDependencies());

        for (Domain.DomainValue domainValue : megaHiddenVar.getDomainValues()) {

            megaHiddenVar.setDomainValue(domainValue);

            AbstractDouble multiplyUnderSum = network.getDoubleFactory().getNew(1.0);

            multiplyUnderSum = multiplyUnderSum.multiply(nextObservation.getProbability());

            Distribution hiddenVarsFullDistribution = backwardFullDistribSaved.get(fullKey);

            // ici il faut travailler sur la distribution sur le parent de l'observation complétée
            // recuperer en fonction de l'ordre les predecesseurs de ce parent
            // et avec la clé comprenant les labels et temps, recuperer la bonne valeur
            // qui vient d'être calculée.
            // pour cela il faudrait enregistrer l'ordre (de markov, pour l'instant on considere identique
            // pour tout le reseau) des dependances dans la variable
            // mais aussi trier les variables de la requete complete par temps et par label

            if (hiddenVarsFullDistribution == null) {

                BackwardRs backwardRs = this.backward(nextObservation.getDependencies(), simpleKey, depth + 1, timeEnd, markovOrder, distribSave);

                hiddenVarsFullDistribution = backwardRs.full;

                backwardFullDistribSaved.put(fullKey, backwardRs.full);
            }

            AbstractDouble combinaisonProb = hiddenVarsFullDistribution.get(megaKeyVar.getDomainValue());

            multiplyUnderSum = multiplyUnderSum.multiply(combinaisonProb.divide(hiddenVarsFullDistribution.getTotal()));

            multiplyUnderSum = multiplyUnderSum.multiply(megaHiddenVar.getProbability());

            sum = sum.add(multiplyUnderSum);
        }

        return sum;
    }

    /**
     * Algorithme backward simplifié qui s'applique sur tout les variables états et toutes les observations
     * à chaque coupe temporelle, les variables sont encapsulées dans une megavariable pour une meilleur
     * lisibilité du code.
     * */
    public Distribution backward(Variable megaState, Variable megaObservation, int time,
                                 Distribution nextBackward, boolean saveBackward) {

        //distribution sur les variable état à l'instant time
        Distribution backward = new Distribution(megaState, network.getDoubleFactory());

        if (time == network.getTime()) {

            //crée une distribution ayant une probabilité de 1 pour chaque valeur du domaine
            for (Domain.DomainValue value : megaState.getDomainValues()) {

                backward.put(value, network.getDoubleFactory().getNew(1.0));
            }

            if (saveBackward) {

                this.backwardMatrices.put(megaState.getVarTimeId(), backward);
            }

            return backward;
        }
        //valeur de domaine à restaurer pour ne pas perturber le processus appelant
        Domain.DomainValue originalValue = megaState.getDomainValue();
        //états à la coupe temporelle suivante
        Variable nextMegaState = network.getMegaState(megaState, time + 1);
        //observation à la coupe temporelle suivante
        Variable nextMegaObservation = null;
        //si le temps suivant est le temps t + 1 courant du dbn il n'y à pas d'observation qui suivent t + 2
        //la fonction sera rappellé avec une variable observation nulle, mais la procedure à la limite ne l'utilise pas
        //sinon on recupère les observations
        if((time + 1) == network.getTime()){

            nextMegaObservation = network.getMegaObs(megaObservation, time + 2);
        }

        //pour chaque valeur de la requete
        for (Domain.DomainValue stateValue : megaState.getDomainValues()) {

            megaState.setDomainValue(stateValue);

            AbstractDouble sum = network.getDoubleFactory().getNew(0.0);

            for(Domain.DomainValue nextStateValue : nextMegaState.getDomainValues()){
                //si le backward n'a pas encore été appelé on fait un appel récursif
                //pour le charger une seule fois.
                if(nextBackward == null){

                    nextBackward = backward(nextMegaState, nextMegaObservation,
                            time + 1, null, saveBackward);
                }

                sum = sum.add(
                        megaObservation.getProbability()//observation
                                .multiply(nextBackward.get(nextStateValue))//backward
                                .multiply(nextMegaState.getProbability()));//transition

            }

            backward.put(stateValue, sum);
        }

        megaState.setDomainValue(originalValue);

        if (saveBackward) {

            this.backwardMatrices.put(megaState.getVarTimeId(), backward);
        }

        return backward;
    }

    private class BackwardRs {

        Distribution normal, full;

        public BackwardRs(Distribution normal, Distribution full) {

            this.normal = normal;

            this.full = full;
        }
    }

    public void showBackward() {

        for (Map.Entry<String, Distribution> entry : this.backwardMatrices.entrySet()) {

            System.out.println(entry.getKey() + entry.getValue());
        }
    }


}
