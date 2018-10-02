package decision;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import inference.dynamic.Util;
import math.Distribution;
import network.FrequencyRange;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.*;


public abstract class PDMPOexploration {

    /*
     * DynamicBayesianNetwork network : nouveau reseau bayesien ou dumoins réinitialisé à zero pour exploration
     * Distribution forward : état de croyance courant sur les états dans le RBD "réel"
     * */

    public static int cptLeaf;

    protected Random rdm = new Random();

    protected AbstractDoubleFactory df;

    protected DynamicBayesianNetwork network;

    protected PDMPO pdmpo;

    protected AbstractDouble minStateProb;

    public PDMPOexploration(AbstractDoubleFactory doubleFactory, double minStateProb) {

        this.df = doubleFactory;

        this.minStateProb = this.df.getNew(minStateProb);
    }

    public PDMPOexploration(AbstractDoubleFactory doubleFactory, DynamicBayesianNetwork dynamicBayesianNetwork, PDMPO pdmpo, double minStateProb) {

        this.df = doubleFactory;

        this.network = dynamicBayesianNetwork;

        this.pdmpo = pdmpo;

        this.minStateProb = df.getNew(minStateProb);
    }

    public PDMPOsearchResult getBestAction(Distribution forward, int limit) {

        cptLeaf = 0;

        return getBestAction(forward, 0, limit, df.getNew(0.0));
    }

    protected PDMPOsearchResult getBestAction(Distribution forward, int time, int limit, AbstractDouble reward) {

        String ident = Util.getIdent(time);

        //System.out.println(ident + "========== BEST ACTION - TIME : " + time + " ===============\n");

        //distribution initiale
        Set<Domain.DomainValue> actions = pdmpo.getActionsFromState(forward, minStateProb);//v

        //si aucune action n'est disponible c'est que le forward ne contient que des positions
        //dont la probabilité est superieure au seuil defini et qui sont finales (but ou echec)
        if (actions.isEmpty()) {

            //dans ce cas retourner une action vide avec une utilité correspondant à l'état final
            return new PDMPOsearchResult(null, pdmpo.getUtility(forward));
        }

        //on etend le network pour le temps time si necessaire
        if (network.getTime() <= time) {

            network.extend();

            //System.out.println(network.toString(ident));
        }
        //utilité fourni par l'action maximum
        AbstractDouble maxActionUtility = network.getDoubleFactory().getNew(Double.NEGATIVE_INFINITY);
        //meilleur action
        Domain.DomainValue bestAction = null;

        //pour chaque action ou combinaison d'actions possible depuis l'état de croyance courant
        //pour une combinaison d'action ou aura un object DomainValue composite
        for (Domain.DomainValue action : actions) {

            AbstractDouble actionUtility = network.getDoubleFactory().getNew(0.0);

            //System.out.println();
            //System.out.println(ident + "--------- ACTION : " + action);
            //crée une liste de percepts possible après avoir appliqué l'action aux états probables
            Map<Domain.DomainValue, AbstractDouble> perceptsMap = getPerceptsProb(forward, action, ident, minStateProb);//v&f
/*
            if (perceptsMap.isEmpty()) {
                //si plus de percepts futur par exemple etats final tres probable et le reste à zero
                //ou en dessous de la limite de consideration
                continue;
            }
*/
            List<Map.Entry<Domain.DomainValue, AbstractDouble>> percepts = filterPercepts(perceptsMap, ident);

            for (Map.Entry<Domain.DomainValue, AbstractDouble> percept : percepts) {

                //System.out.println(ident + "--------- SAMPLE PERCEPT : " + samplePercept);

                AbstractDouble perceptProb = percept.getValue();

                //recupere la variable (ou megavariable) action et percept
                Variable actionVar = pdmpo.getActionVar();

                Variable perceptVar = pdmpo.getPerceptVar();
                //initialise la valeur, si DomainValue composite les variables composant
                //la megavariable seront initialisées
                actionVar.setDomainValue(action);

                perceptVar.setDomainValue(percept.getKey());
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
                //tant que l'on a pas atteind un état final
                if (time < limit) {
                    //rapelle la fonction avec le nouvel état de croyance et la recompense obtenu ajouté à la précedente
                    //recupère la meilleure action et son l'utilité
                    PDMPOsearchResult rs = getBestAction(nextForward, time + 1, limit,
                            currentTotalReward);
                    //utilité fourni par la meilleur action
                    currentTotalReward = rs.bestUtility;
                }

                actionUtility = actionUtility.add(perceptProb.multiply(currentTotalReward));
            }
            //System.out.println(ident + "CURRENT REWARD " + forwardReward + " + " + reward + " = " + currentTotalReward);

            if (actionUtility.compareTo(maxActionUtility) > 0) {

                maxActionUtility = actionUtility;

                bestAction = action;
            }

            cptLeaf++;

            //System.out.println("UTILITE ESPERE : " + actionUtility);
        }

        return new PDMPOsearchResult(bestAction, maxActionUtility);
    }

    protected Map<Domain.DomainValue, AbstractDouble> getPerceptsProb(Distribution forward,
                                                                      Domain.DomainValue action, String ident,
                                                                      AbstractDouble minStateProb) {

        Map<Domain.DomainValue, AbstractDouble> perceptsMap = new Hashtable<>();

        //pour chaque état
        for (Domain.DomainValue state : forward.getRowValues()) {

            AbstractDouble stateProb = forward.get(state);
            //si cet etat à une probabilité superieur au seuil minimum par exemple 1/50 ou 1/100
            if (stateProb.compareTo(minStateProb) > 0) {

                //System.out.println();
                //System.out.println("--> ETAT DE DEPART : " + state);
                //on applique l'action à un des états de l'état de croyance complet
                //et on obtient les états resultants avec leur probabilités si action non deterministe

                Collection<PDMPO.RsState> rsStates = pdmpo.getResultStates(state, action);//v

                for (PDMPO.RsState rsState : rsStates) {

                    // //System.out.println("<-- ETAT D' ARRIVEE : " + rsState.getState() + " - PROB : " + rsState.getProb());

                    //chaque etat resultat fourni un percept
                    //la probabilité de ce percept et la probabilité de l'état precedent
                    //multiplié par la probabilite de l'état resultant de l'action
                    //si plusieurs etats resultant fournissent le même percepts leur
                    //probabilités sont additionés
                    //!!! probleme les probabilités des percepts ici ne tiennent pas compte du
                    //bruit dans le scapteurs representés dans la TCP des observations ...

                    Domain.DomainValue percept = pdmpo.getPerceptFromState(rsState.getState());//v&f

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

    protected abstract List<Map.Entry<Domain.DomainValue, AbstractDouble>> filterPercepts(Map<Domain.DomainValue, AbstractDouble> perceptsMap, String ident);

    public void setNetwork(DynamicBayesianNetwork network) {
        this.network = network;
    }

    public void setPdmpo(PDMPO pdmpo) {
        this.pdmpo = pdmpo;
    }

    public void setMinStateProb(AbstractDouble minStateProb) {
        this.minStateProb = minStateProb;
    }
}
