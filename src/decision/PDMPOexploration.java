package decision;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import inference.dynamic.Util;
import math.Distribution;
import network.FrequencyRange;
import network.ProbabilityComputeFromTCP;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.*;


public class PDMPOexploration {

    /*
     * DynamicBayesianNetwork network : nouveau reseau bayesien ou dumoins réinitialisé à zero pour exploration
     * Distribution forward : état de croyance courant sur les états dans le RBD "réel"
     * */

    public static int cptLeaf;

    private Random rdm = new Random();

    public PDMPOsearchResult getBestActionPerceptSampling(DynamicBayesianNetwork network, PDMPO pdmpo, Distribution forward, int limit) {

        cptLeaf = 0;

        return getBestActionPerceptSampling(network, pdmpo, forward, 0, limit, network.getDoubleFactory().getNew(0.0));
    }


    private PDMPOsearchResult getBestActionPerceptSampling(DynamicBayesianNetwork network, PDMPO pdmpo, Distribution forward,
                                                           int time, int limit, AbstractDouble reward) {

        String ident = Util.getIdent(time);

        //System.out.println(ident + "========== BEST ACTION - TIME : " + time + " ===============\n");

        //on etend le network pour le temps time si necessaire
        if (network.getTime() <= time) {

            network.extend();

            //System.out.println(network.toString(ident));
        }

        //distribution initiale
        Set<Domain.DomainValue> actions = pdmpo.getActionsFromState(forward);

        AbstractDouble maxActionUtility = network.getDoubleFactory().getNew(Double.NEGATIVE_INFINITY);

        Domain.DomainValue bestAction = null;

        //pour chaque action ou combinaison d'actions possible depuis l'état de croyance courant
        //pour une combinaison d'action ou aura un object DomainValue composite
        for (Domain.DomainValue action : actions) {

            //System.out.println();
            //System.out.println(ident + "--------- ACTION : " + action);
            //crée une liste de percepts possible après avoir appliqué l'action aux états probables
            Map<Domain.DomainValue, AbstractDouble> perceptsMap = getPerceptsProb(forward, pdmpo, action, ident);

            AbstractDouble actionUtility = network.getDoubleFactory().getNew(0.0);

            Map.Entry<Domain.DomainValue, AbstractDouble> samplePercept = this.getSamplePercept(perceptsMap, network, ident);

            //System.out.println(ident + "--------- SAMPLE PERCEPT : " + samplePercept);

            AbstractDouble perceptProb = samplePercept.getValue();

            //recupere la variable (ou megavariable) action et percept
            Variable actionVar = pdmpo.getActionVar();

            Variable perceptVar = pdmpo.getPerceptVar();
            //initialise la valeur, si DomainValue composite les variables composant
            //la megavariable seront initialisées
            actionVar.setDomainValue(action);

            perceptVar.setDomainValue(samplePercept.getKey());
            //initialise l'action pour le temps courant
            network.initVar(time, actionVar);
            //initialise le precept pour le temps suivant pour lequel calculer la distribution d'état
            network.initVar(time + 1, perceptVar);

            //System.out.println("ACTION : "+action+" - PERCEPT : "+samplePercept.getKey()+", "+samplePercept.getValue());

            //System.out.println(network);
            //calcule la distribution futur en fonction d'une action et d'un percept possible donné
            Distribution nextForward = network.forward(pdmpo.getStates(), pdmpo.getActions(), time + 1, forward);

            //System.out.println(nextForward.toString(ident));

            //System.out.println(nextForward);
            //récompense de l'états de croyance obtenu
            AbstractDouble forwardReward = pdmpo.getUtility(nextForward);
            //recompense totale l'ancienne plus la courante
            AbstractDouble currentTotalReward = forwardReward.add(reward);
            //tant que le temps est inferieur à la limite
            if (time < limit) {
                //rapelle la fonction avec le nouvel état de croyance et la recompense obtenu ajouté à la précedente
                //recupère la meilleure action et son l'utilité
                PDMPOsearchResult rs = getBestActionPerceptSampling(network, pdmpo, nextForward, time + 1, limit, currentTotalReward);
                //utilité fourni par la meilleur action
                currentTotalReward = rs.bestUtility;
            }

            //System.out.println(ident + "CURRENT REWARD " + forwardReward + " + " + reward + " = " + currentTotalReward);

            if (currentTotalReward.compareTo(maxActionUtility) > 0) {

                maxActionUtility = currentTotalReward;

                bestAction = action;
            }

            cptLeaf++;

            //System.out.println("UTILITE ESPERE : " + actionUtility);
        }

        return new PDMPOsearchResult(bestAction, maxActionUtility);
    }

    private Map.Entry<Domain.DomainValue, AbstractDouble> getSamplePercept(Map<Domain.DomainValue, AbstractDouble> perceptsMap,
                                                                           DynamicBayesianNetwork network, String ident) {

        //probabilités des percepts cumule à partir de 0
        AbstractDouble cumul = network.getDoubleFactory().getNew(0.0);
        //tableau pour recherche dichotomique d'entrées (DomainValue:Range de probabilité)
        List<Map.Entry<Domain.DomainValue, FrequencyRange>> frequencies = Arrays.asList(new Map.Entry[perceptsMap.size()]);

        int i = 0;

        for (Map.Entry<Domain.DomainValue, AbstractDouble> percept : perceptsMap.entrySet()) {
            //initialise un object range min prob max pro
            FrequencyRange range = new FrequencyRange();

            range.setMin(cumul);
            //ajoute la frequence du percept au cumul
            cumul = cumul.add(percept.getValue());
            //donne la frequence max pour ce percept et la frequence min pour le prochain
            range.setMax(cumul);
            //enregistre l'entrée
            frequencies.set(i, new AbstractMap.SimpleEntry(percept.getKey(), range));

            i++;
        }

        double rdmDouble = rdm.nextDouble();

        //System.out.println(ident + " " + rdmDouble);

        //System.out.println(ident + " " + frequencies);

        Domain.DomainValue samplePercept = FrequencyRange.dichotomicSearch(frequencies, network.getDoubleFactory().getNew(rdmDouble));

        return new AbstractMap.SimpleEntry<>(samplePercept, perceptsMap.get(samplePercept));
    }


    public PDMPOsearchResult getBestAction(DynamicBayesianNetwork network, PDMPO pdmpo, Distribution forward, int limit, double maxPerceptProb) {

        cptLeaf = 0;

        return getBestAction(network, pdmpo, forward, 0, limit, network.getDoubleFactory().getNew(0.0), network.getDoubleFactory().getNew(maxPerceptProb));
    }

    private PDMPOsearchResult getBestAction(DynamicBayesianNetwork network, PDMPO pdmpo, Distribution forward,
                                            int time, int limit, AbstractDouble reward, AbstractDouble maxPerceptProb) {

       // String ident = Util.getIdent(time);

       // //System.out.println(ident + "========== BEST ACTION - TIME : " + time + " ===============\n");

        //on etend le network pour le temps time si necessaire
        if (network.getTime() <= time) {

            network.extend();

            //System.out.println(network.toString(ident));
        }

        //distribution initiale
        Set<Domain.DomainValue> actions = pdmpo.getActionsFromState(forward);

        AbstractDouble maxActionUtility = network.getDoubleFactory().getNew(Double.NEGATIVE_INFINITY);

        Domain.DomainValue bestAction = null;

        //pour chaque action ou combinaison d'actions possible depuis l'état de croyance courant
        //pour une combinaison d'action ou aura un object DomainValue composite
        for (Domain.DomainValue action : actions) {

          //  //System.out.println();
          //  //System.out.println(ident + "--------- ACTION : " + action);
            //crée une liste de percepts possible après avoir appliqué l'action aux états probables
            Map<Domain.DomainValue, AbstractDouble> perceptsMap = getPerceptsProb(forward, pdmpo, action, "");

            List<Map.Entry<Domain.DomainValue, AbstractDouble>> bestPerceptsList = selectBestPercepts(perceptsMap, maxPerceptProb, "", network.getDoubleFactory());

            AbstractDouble actionUtility = network.getDoubleFactory().getNew(0.0);

            for (Map.Entry<Domain.DomainValue, AbstractDouble> entry : bestPerceptsList) {

                AbstractDouble perceptProb = entry.getValue();

                //recupere la variable (ou megavariable) action et percept
                Variable actionVar = pdmpo.getActionVar();

                Variable perceptVar = pdmpo.getPerceptVar();
                //initialise la valeur, si DomainValue composite les variables composant
                //la megavariable seront initialisées
                actionVar.setDomainValue(action);

                perceptVar.setDomainValue(entry.getKey());
                //initialise l'action pour le temps courant
                network.initVar(time, actionVar);
                //initialise le precept pour le temps suivant pour lequel calculer la distribution d'état
                network.initVar(time + 1, perceptVar);

                //System.out.println(network);
                //calcule la distribution futur en fonction d'une action et d'un percept possible donné
                Distribution nextForward = network.forward(pdmpo.getStates(), pdmpo.getActions(), time + 1, forward);

               // //System.out.println(nextForward.toString(ident));

                //récompense de l'états de croyance obtenu
                AbstractDouble forwardReward = pdmpo.getUtility(nextForward);
                //recompense totale l'ancienne plus la courante
                AbstractDouble currentTotalReward = forwardReward.add(reward);
                //tant que le temps est inferieur à la limite
                if (time < limit) {
                    //rapelle la fonction avec le nouvel état de croyance et la recompense obtenu ajouté à la précedente
                    //recupère la meilleure action et son l'utilité
                    PDMPOsearchResult rs = getBestAction(network, pdmpo, nextForward, time + 1, limit, currentTotalReward, maxPerceptProb);
                    //utilité fourni par la meilleur action
                    currentTotalReward = rs.bestUtility;
                }

                cptLeaf++;

                //on multiplie l'utilité retournée par la meilleur action par la probabilité du percept
                //on additionne tout les resultats pour obtenir l'utilité moyenne par action
                actionUtility = actionUtility.add(perceptProb.multiply(currentTotalReward));
            }

            if (actionUtility.compareTo(maxActionUtility) > 0) {

                maxActionUtility = actionUtility;

                bestAction = action;
            }

            //System.out.println("UTILITE ESPERE : " + actionUtility);
        }

        return new PDMPOsearchResult(bestAction, maxActionUtility);
    }

    private List<Map.Entry<Domain.DomainValue, AbstractDouble>> selectBestPercepts(
            Map<Domain.DomainValue, AbstractDouble> perceptsMap,
            AbstractDouble maxPerceptProb, String ident,
            AbstractDoubleFactory doubleFactory) {

        //crée un tableau à partir des entrées percept:prob
        List<Map.Entry<Domain.DomainValue, AbstractDouble>> perceptsTab = new ArrayList<>(perceptsMap.entrySet());
        //quick sort du tableau
        Collections.sort(perceptsTab, new Comparator<Map.Entry<Domain.DomainValue, AbstractDouble>>() {
            @Override
            public int compare(Map.Entry<Domain.DomainValue, AbstractDouble> o1, Map.Entry<Domain.DomainValue, AbstractDouble> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        //récupération des percepts les plus probables juqu'à atteindre un certain seul max

       // //System.out.println(ident+" Percepts sorted "+perceptsTab);

        //indice de fin exclusif du sous tableau de eprcepts
        int maxIndex = 1;

        AbstractDouble totalProb = doubleFactory.getNew(0.0);

        for (Map.Entry<Domain.DomainValue, AbstractDouble> perceptProb : perceptsTab) {
            //ajoute la probabilité des percepts dans l'ordre decroissant
            totalProb = totalProb.add(perceptProb.getValue());

            maxIndex++;
            //si on atteind le seuil limite on arrete
            if (totalProb.compareTo(maxPerceptProb) >= 0) {

                break;
            }
        }

        //System.out.println(ident+" Percepts sorted sublist before normalisation : "+perceptsTab.subList(0, maxIndex));

        //normaliser les probabilités de la sous liste pour obtenir un total de 100 %
        for(Map.Entry<Domain.DomainValue, AbstractDouble> percept : perceptsTab.subList(0, maxIndex)){

            percept.setValue(percept.getValue().divide(totalProb));
        }

      //  //System.out.println(ident+" Percepts sorted sublist normalise : "+perceptsTab.subList(0, maxIndex));

        return perceptsTab.subList(0, maxIndex);
    }

    private Map<Domain.DomainValue, AbstractDouble> getPerceptsProb(Distribution forward, PDMPO pdmpo, Domain.DomainValue action, String ident) {

        Map<Domain.DomainValue, AbstractDouble> perceptsMap = new Hashtable<>();

        //pour chaque état
        for (Domain.DomainValue state : forward.getRowValues()) {

            AbstractDouble stateProb = forward.get(state);
            //si cet etat à une probabilité superieur à zero
            if (stateProb.getDoubleValue().compareTo(0.0) > 0) {

                //  //System.out.println();
                //  //System.out.println("--> ETAT DE DEPART : " + state);
                //on applique l'action à un état de l'état de croyance complet
                //et on obtient les resultants avec leur probabilités si action non deterministe
                for (PDMPO.RsState rsState : pdmpo.getResultStates(state, action)) {

                    // //System.out.println("<-- ETAT D' ARRIVEE : " + rsState.getState() + " - PROB : " + rsState.getProb());

                    //chaque etat resultat fourni un percept
                    //la probabilité de ce percept et la probabilité de l'état precedent
                    //multiplié par la probabilite de l'état resultant de l'action
                    //si plusieurs etats resultant fournissent le même percepts leur
                    //probabilités sont additionés

                    Domain.DomainValue percept = pdmpo.getPerceptFromState(rsState.getState());

                    //System.out.println("(0) PERCEPT " + percept);

                    AbstractDouble rsStatesProb = stateProb.multiply(rsState.getProb());

                    //System.out.println("COMPUTE : "+stateProb+" * "+rsState.getProb());

                    if (perceptsMap.containsKey(percept)) {

                        perceptsMap.put(percept, perceptsMap.get(percept).add(rsStatesProb));

                    } else {

                        perceptsMap.put(percept, rsStatesProb);
                    }

                    //System.out.println("TOTAL PROB PERCEPT " + perceptsMap.get(percept));
                }
            }
        }
/*
        for (Map.Entry entry : perceptsMap.entrySet()) {

            //System.out.println(ident + entry.getKey() + " : " + entry.getValue());
        }
*/
        return perceptsMap;
    }

    public class PDMPOsearchResult {

        private Domain.DomainValue action;
        private AbstractDouble bestUtility;

        public PDMPOsearchResult(Domain.DomainValue action, AbstractDouble bestUtility) {
            this.action = action;
            this.bestUtility = bestUtility;
        }

        public Domain.DomainValue getAction() {
            return action;
        }

        public void setAction(Domain.DomainValue action) {
            this.action = action;
        }

        public AbstractDouble getBestUtility() {
            return bestUtility;
        }

        public void setBestUtility(AbstractDouble bestUtility) {
            this.bestUtility = bestUtility;
        }

        @Override
        public String toString() {
            return "PDMPOsearchResult{" +
                    "action=" + action +
                    ", bestUtility=" + bestUtility +
                    '}';
        }
    }

}
