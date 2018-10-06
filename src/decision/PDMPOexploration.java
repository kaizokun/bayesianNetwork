package decision;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import inference.dynamic.Util;
import math.Distribution;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.*;


public abstract class PDMPOexploration {

    /*
     * DynamicBayesianNetwork network : nouveau reseau bayesien ou dumoins réinitialisé à zero pour exploration
     * Distribution forward : état de croyance courant sur les états dans le RBD "réel"
     * */

    public static boolean showlog = false;

    public static int cptLeaf, cptPercepts, cptLoopState;

    protected Random rdm = new Random();

    protected AbstractDoubleFactory df;

    protected DynamicBayesianNetwork network;

    protected PDMPO pdmpo;

    protected AbstractDouble minStateProb, minRiskProb, minPerceptProb;

    protected Map<String, PDMPOsearchResult> resultsSaved = new Hashtable<>();

    protected Comparator<ActionResult> actionResultComparator;

    {
        actionResultComparator = new Comparator<ActionResult>() {
            @Override
            public int compare(ActionResult o1, ActionResult o2) {
                return o2.estimation.compareTo(o1.estimation);
            }
        };
    }

    public void resetSharedResults() {

        this.resultsSaved.clear();
    }

    public PDMPOexploration(AbstractDoubleFactory doubleFactory, double minStateProb, double minRiskProb, double minPerceptProb) {

        this.df = doubleFactory;

        this.minStateProb = this.df.getNew(minStateProb);

        this.minRiskProb = this.df.getNew(minRiskProb);

        this.minPerceptProb = this.df.getNew(minPerceptProb);
    }

    public PDMPOexploration(AbstractDoubleFactory doubleFactory, DynamicBayesianNetwork dynamicBayesianNetwork, PDMPO pdmpo, double minStateProb) {

        this.df = doubleFactory;

        this.network = dynamicBayesianNetwork;

        this.pdmpo = pdmpo;

        this.minStateProb = df.getNew(minStateProb);
    }

    public PDMPOsearchResult getBestAction(Distribution forward) {

        cptLeaf = 0;

        cptPercepts = 0;

        cptLoopState = 0;

        PDMPOsearchResult rs;

        int limit = 0;

        AbstractDouble lastUtility = df.getNew(Double.NEGATIVE_INFINITY);

        boolean nextLimit = true;

        do {

            // MAP visited
            // contient une clé approximative d'un état est la récompense pur de cet état
            //permet de detecter le retour sur un état (boucle)
            //on a ici besoin d'enregistrer également l'utilité de cet état pur
            //pour l'ajouter à la recompense courante quand on le rencontre à nouveau

            //pour la MAP savedResults
            //même principe que pour la map visited à la difference
            //qu'on enregistre le resultat fournit par un état plus l'utilité fourni par l'exploration qui suit
            //sans le reward qui le precede. permet de retrouver la meilleur utilité fourni par une action
            //depuis un état de croyance si on le retrouve dans plusieurs explorations en profondeur differentes
            //l'utilité est enregistré pour un états de royance avec l'action et le temps auquel l'état à été rencontré
            //en effet avec une exploration limité un état exploré plutot peut aller plus en profondeur
            //et donc calculer une utilité plus precise. Pendant l'exploration si on recontre un forward mais
            //que son utilité est enregistré pour un temps superieur on effectue à nouveau l'exploration
            //et on ecrase l'ancienne utilité enregistrée

            //enregistrer le forward de base pour eviter les boucles des le depart
            //depend de la precision de la clé !
            rs = getBestAction(forward, 0, limit, df.getNew(0.0), new Hashtable<>(), new Hashtable<>(), new LinkedList<>());
            //pour environnement statique les resultats sauvegardé restent valables

            // System.out.println("NEW LIMIT : " + limit + " - RESULT : " + rs);

            limit++;

            //tant que l'utilité est négative
            //des qu'elle est positive c'est que l'on a atteind un but
            //et l'exploration étant en profondeur limité on sait qu'on a atteind le but le plus proche

        } while (rs.getBestUtility().getDoubleValue() < 0.0);

        //System.out.println("BEST ACTION " + rs);

        // System.exit(0);

        return rs;
    }

    protected PDMPOsearchResult getBestAction(Distribution forward, int time, int limit, AbstractDouble reward,
                                              Map<String, AbstractDouble> visited,
                                              Map<String, PDMPOsearchResult> savedResults, LinkedList<Domain.DomainValue> lastActions) {
        String ident = "";

        if (showlog) {

            ident = Util.getIdent(time);
        }

        boolean isGoal = pdmpo.isGoal(forward);

        //limite atteinte but non atteind
        //estimation
        if (time > limit && !isGoal) {
            //l'estimation fourni de bons resultats même avec une limite
            //de profondeur courte
            // AbstractDouble estimation = pdmpo.getEstimationForward(forward);

            AbstractDouble estimation = pdmpo.getUtility(forward);

            if (showlog) {

                System.out.println(ident + "LIMIT " + time + " " + estimation);

                System.out.println(ident + "ACTIONS " + lastActions);
            }

            cptLeaf++;

            //on ajoute le reward courant à l'utilité de l'estimation
            return new PDMPOsearchResult(pdmpo.getNoAction(), reward.add(estimation));
        }
        //on ajoute le reward courant à l'utilité du forward
        AbstractDouble forwardReward = pdmpo.getUtility(forward);

        reward = reward.add(forwardReward);
        //limite non atteinte but atteint
        if (isGoal) {

            if (showlog) {

                System.out.println(ident + "GOAL : " + time + " = " + reward);

                System.out.println("ACTIONS " + lastActions);

                //System.out.println(forward);
            }

            cptLeaf++;

            return new PDMPOsearchResult(pdmpo.getNoAction(), reward, true);
        }

        if (showlog) {

            System.out.println(ident + "========== BEST ACTION - TIME : " + time + " ===============\n");

            if (showlog)
                System.out.println(ident + "FORWARD UTILITY : " + forwardReward);

            if (showlog)
                System.out.println(ident + "CURRENT REWARD : " + reward);
        }

        String forwardKey = pdmpo.getKeyForward(forward);
        //on associe l'utilité pur de l'état de croyance à la clé approximative
        visited.put(forwardKey, forwardReward);

        //distribution initiale
        Set<Domain.DomainValue> actions = pdmpo.getActionsFromState(forward, minStateProb);//v

        //on etend le network pour le temps time si necessaire
        if (network.getTime() <= time) {

            network.extend();
/*
            if (showlog)
                System.out.println(network.toString(ident));*/
        }

        boolean exploration = false;

        //liste d'actions avec leur resultat sans les percepts et l'estimation
        List<ActionResult> actionResults = new ArrayList<>(actions.size());

        for (Domain.DomainValue action : actions) {

            //initialisation de la valeur de la variable action du reseau
            Variable actionVar = pdmpo.getActionVar();

            actionVar.setDomainValue(action);

            network.initVar(time, actionVar);
            //reset de la variable percept pour prevision
            network.resetVar(time + 1, pdmpo.getPerceptVar());
            //calcul de la prevision d'état
            Distribution forwardPrevision = network.forward(pdmpo.getStates(), pdmpo.getActions(), time + 1, forward);
            //vérifie si l'état de croyance comporte des états finaux d'echec ayant une probabilité supérieur à un seuil
            //donné, dans ce cas l'action doit être évité
            if (pdmpo.iStateOfBelieveRisky(forwardPrevision, minRiskProb)) {

                if (showlog) System.out.println("\n" + ident + "!!! FORWARD RISKY !!!");
                continue;
            }
            //ajout du resultat de l'action avec l'estimation de son utilité
            actionResults.add(new ActionResult(action, forwardPrevision, pdmpo.getEstimationForward(forwardPrevision)));
        }
        //trie decroissant par estimation
        Collections.sort(actionResults, actionResultComparator);

        if (showlog) {
            System.out.println();
            System.out.println(ident + "ALL ACTIONS [" + time + "]: " + actionResults);
        }

        //utilité fourni par l'action maximum
        AbstractDouble maxActionUtility = network.getDoubleFactory().getNew(Double.NEGATIVE_INFINITY);
        //meilleur action
        Domain.DomainValue bestAction = null;
        //l'action mène à tout les coups vers un but
        boolean bestActionReachGoal = false;

        //pour chaque action ou combinaison d'actions possible depuis l'état de croyance courant
        //pour une combinaison d'action ou aura un object DomainValue composite
        for (ActionResult actionRs : actionResults) {

            lastActions.addLast(actionRs.action);

            //initialisation de la valeur de la variable action du reseau
            Variable actionVar = pdmpo.getActionVar();

            actionVar.setDomainValue(actionRs.action);

            network.initVar(time, actionVar);

            AbstractDouble actionUtility = network.getDoubleFactory().getNew(0.0);

            if (showlog) {
                System.out.println();
                System.out.println(ident + "[>>>] ACTION [" + time + "] : " + actionRs.action + " - LAST REWARD : " + reward);
            }
            //Distribution forwardPrevision = network.forward(pdmpo.getStates(), pdmpo.getActions(), time + 1, forward);
            Distribution forwardPrevision = actionRs.forwardPrevision;

            Map<Domain.DomainValue, AbstractDouble> perceptsMap = this.loadPerceptsPrevision(forwardPrevision, time, ident);//v

            //liste des percepts possible avec probabilité en fonction de l'état de croyance previsionel
            //et du bruit dans les capteurs definies dans le reseau bayesien
            List<Map.Entry<Domain.DomainValue, AbstractDouble>> percepts = this.filterPercepts(perceptsMap, ident);

            if (showlog)
                System.out.println(ident + "(0) ALL PERCEPTS (" + percepts.size() + ") : " + percepts);

            if (showlog)
                System.out.println(ident + "PERCEPTS MAP :  " + perceptsMap);

            cptPercepts += percepts.size();

            int iPercept = 0;

            boolean allGoals = true;

            for (Map.Entry<Domain.DomainValue, AbstractDouble> percept : percepts) {

                AbstractDouble perceptProb = percept.getValue();
                //recupere la variable  percept
                Variable perceptVar = pdmpo.getPerceptVar();

                perceptVar.setDomainValue(percept.getKey());
                //initialise le precept pour le temps suivant pour lequel calculer la distribution d'état
                network.initVar(time + 1, perceptVar);

                if (showlog)
                    System.out.println(ident + "(0) PERCEPT [" + iPercept + "] : " + percept.getKey() + ", " + percept.getValue());

                //calcule la distribution futur en fonction d'une action et d'un percept possible donné
                Distribution nextForward = network.forward(pdmpo.getStates(), pdmpo.getActions(), time + 1, forward);

                //clé approximative du next forward
                String nextForwardKey = pdmpo.getKeyForward(nextForward);

                if (showlog) {

                    System.out.println(ident + "KEY VISITED : " + nextForwardKey);
                }

                AbstractDouble exploReward;
                //le système d'approcimation sur les états de croyance pour eviter les loops
                //fonctionne sans limite imposé laprecision est reglable dans le pdmpo à 1 ou plusieurs
                //chiffre après la virgule ce qui crée un ensemble pouvant englober plusieurs états de croyances proches
                if (!visited.containsKey(nextForwardKey)) {
                    //rapelle la fonction avec le nouvel état de croyance et la recompense obtenu ajouté à la précedente
                    //recupère la meilleure action et son l'utilité
                    //verifie si on a déja fait une exploration à partir d'un état de croyance proche

                    PDMPOsearchResult rs;

                    //Si un état de croyance n'a pas déja été évalué ( au temps suivant (time + 1) )
                    //ou qu'il l'a été à un temps supérieur une exploration limité à partir d'un temps inférieur
                    //pourrait fournir une meilleur approximation
                    if (!savedResults.containsKey(nextForwardKey) || savedResults.get(nextForwardKey).getTime() > time + 1) {
                        //marque l'état de croyance comme visité pour le parcour courant

                        if (showlog)
                            System.out.println(ident + "FIRST VISIT " + nextForwardKey);

                        //permet de savoir si une epxloration au moins a eu lieu lors de cet appel
                        exploration = true;

                        rs = getBestAction(nextForward, time + 1, limit,
                                reward, visited, savedResults, lastActions);

                        //enregistre l'utilité de la meilleur action depuis le forward (approximation)
                        //pour l'utiliser dans les autres parcours plutot que de recalculer les utilités
                        //peut poser des problèmes en provoquant des loops (raison à determiner)
                        //loop peuvent être éviter si on demarre en indiquant la precedente action
                        //et qu'on evite les actions qui retourne en arriere
                        //mais pas forcement le fonctionnement général à adopter dans tout les cas

                        //on enregitre pour la clé du prochain état de croyance l'utilité DEPUIS cet état de croyance,
                        //(donc sans le reward courant) et l'action qui l'a fourni.
                        //Si une meilleur évaluation d'un état de croyance (çàd exploré à partir d'un temps inférieur permettant une
                        //exploration plus profonde) l'utilité sera écrasé
                        savedResults.put(
                                nextForwardKey,
                                new PDMPOsearchResult(
                                        rs.getAction(),
                                        rs.getBestUtility().substract(reward),
                                        time + 1, rs.isGoal));

                        exploReward = rs.getBestUtility();
                        //demarque l'état de croyance comme visité
                    } else {
                        //etat de croyance déja visité lors d'une autre exploration en profondeur
                        //retourne l'utilité et action enregistré DEPUIS cet état de croyance
                        rs = savedResults.get(nextForwardKey);
                        //on ajoute la recompense courante
                        exploReward = rs.getBestUtility().add(reward);

                        if (showlog) {
                            System.out.println(ident + time + " RS STATE SAVED " + rs);

                            System.out.println(ident + nextForwardKey);
                        }
                    }

                    //si un des resultats n'est pas un but on testera une autre action
                    if (!rs.isGoal) {

                        allGoals = false;
                    }

                    if (showlog)
                        System.out.println(ident + "UTILITE RETOURNE " + exploReward);

                } else {

                    cptLoopState++;

                    if (showlog) {
                        System.out.println(ident + "(0) VISITED ");
                        System.out.println(ident + "REWARD VISITED STATE : " + visited.get(nextForwardKey));
                    }
                    //si état déja visité lors du parcour courant (boucle) on ajoute son utilité pur au reward courant
                    exploReward = reward.add(visited.get(nextForwardKey));
                    //logiquement si un etats de croyance affiné par un percept est une loupe on a pas atteind le but...
                    allGoals = false;
                }
                //pour chaque percept fourni par le résultat d'une action sur l'état de croyance
                //on pondere l'utilité de l'exploration fourni l' état de croyance résultat,
                //par la probabilité du percept qui l'a fourni. on additionne le tout pour
                //obtenir l'utilité de l'action
                actionUtility = actionUtility.add(perceptProb.multiply(exploReward));

                if (showlog) {

                    System.out.println(ident + "ACTION UTILITY " + actionRs.action + " : " + perceptProb + " * " + exploReward + " =  " + (perceptProb.multiply(exploReward)));
                }

                iPercept++;
            }

            if (actionUtility.compareTo(maxActionUtility) > 0) {

                maxActionUtility = actionUtility;

                bestAction = actionRs.action;

                bestActionReachGoal = allGoals;
            }

            if (showlog)
                System.out.println(ident + "UTILITE ESPERE ACTION : " + actionRs.action + " = " + actionUtility);

            lastActions.removeLast();

            //si l'action mène à un but à tout les coups
            //on en teste pas d'autres elle sont censé être trié par ordre de bénéfice
            if (allGoals) {

                //System.out.println("ALLGOALS ");

                break;
            }
        }

        visited.remove(forwardKey);

        if (showlog)
            System.out.println(ident + "MAX UTILITE ESPERE ACTION : " + bestAction + " = " + maxActionUtility);


        if (!exploration) {
            //si aucune exploration n'a eu lieu sans qu'on ai atteind un but ni une limite on compte une feuille
            //cette situation peut arriver pour plusieurs raisons : pas d'actions, états déja visité
            //dans le parcour courant, ou dans un parcour precedent.
            cptLeaf++;
        }

        return new PDMPOsearchResult(bestAction, maxActionUtility, bestActionReachGoal);
    }

    protected Map<Domain.DomainValue, AbstractDouble> loadPerceptsPrevision(Distribution forwardPrevision,
                                                                            int time,
                                                                            String ident) {

        //System.out.println(forwardPrevision);
        //table des percepts
        Map<Domain.DomainValue, AbstractDouble> perceptsMap = new Hashtable<>();
        //variable percept pour recuperer la TCP
        Variable perceptVar = network.getVariable(time + 1, pdmpo.getPerceptVar());
        //TCP variable Percept
        ProbabilityCompute tcpPercept = perceptVar.getProbabilityCompute();
        //pour chaque états
        for (Domain.DomainValue state : forwardPrevision.getRowValues()) {
            //probabilité de l'état
            AbstractDouble stateProb = forwardPrevision.get(state);
            //récupère l'entrée de la TCP des percepts dependent des états correspondant à l'état courant
            Map<Domain.DomainValue, AbstractDouble> statePerceptsProbs = tcpPercept.getTCP().get(tcpPercept.getValueKey(state));

            //pour chaque percept
            for (Domain.DomainValue percept : pdmpo.getPercepts()) {

                //probabilité du percept en fonction de l'état
                //la probabilité du percept est multipliée par celle de l'état qui le fournit
                AbstractDouble statePerceptProb = statePerceptsProbs.get(percept).multiply(stateProb);
                //recupere la probabilité enregistré dans la map
                AbstractDouble perceptProb = perceptsMap.get(percept);
                //si pas encore enregistré on l'initialise
                if (perceptProb == null) {

                    perceptProb = statePerceptProb;
                    //sinon on cumule les probabilités
                } else {

                    perceptProb = perceptProb.add(statePerceptProb);
                }

                //reste à mettre à jour la map
                perceptsMap.put(percept, perceptProb);
            }
        }

        //total des probabilités des percepts
        AbstractDouble total = df.getNew(0.0);
        //percepts à retirer
        Set<Domain.DomainValue> removePercepts = new HashSet<>();

        for (Map.Entry<Domain.DomainValue, AbstractDouble> entry : perceptsMap.entrySet()) {

            if (entry.getValue().compareTo(minPerceptProb) < 0) {
                //ajout du percept à retirer si probabilité en dessous du seuil
                removePercepts.add(entry.getKey());
            } else {
                //sinon on ajoute la probabilité au total
                total = total.add(entry.getValue());
            }
        }
        //retrait des percepts hautement improbables
        for (Domain.DomainValue percept : removePercepts) {

            perceptsMap.remove(percept);
        }

        //normalisation des percepts sur 100%
        for (Map.Entry<Domain.DomainValue, AbstractDouble> entry : perceptsMap.entrySet()) {

            entry.setValue(entry.getValue().divide(total));
        }

        return perceptsMap;
    }

    //methode de recuperation des percepts mais moins rigoureuse que loadPerceptsPrevision
    //qui tiens compte du bruit dans les capteurs simulé dans la TCP des percepts
    //mais genere 10 fois moins de percepts
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
/*
        AbstractDouble total = df.getNew(0.0);
        //calcule le total pour normaliser au cas ou la distribution ne l'est pas
        //les percepts sont ponderés pour simuler le bruit dans les capteurs.
        for(Map.Entry<Domain.DomainValue,AbstractDouble> entry : perceptsMap.entrySet()){

            total = total.add(entry.getValue().multiply(pdmpo.getProbRightPercept(entry.getKey())));
        }

        for(Map.Entry<Domain.DomainValue,AbstractDouble> entry : perceptsMap.entrySet()){

            entry.setValue(entry.getValue().divide(total));
        }
*/

        //System.out.println("METHODE 1");
        // System.out.println(Util.printMap(perceptsMap));

        return perceptsMap;
    }

    public class PDMPOsearchResult {

        private Domain.DomainValue action;

        private AbstractDouble bestUtility;

        private boolean isGoal;

        private int time;

        public PDMPOsearchResult(Domain.DomainValue action, AbstractDouble bestUtility, boolean isGoal) {
            this.action = action;
            this.bestUtility = bestUtility;
            this.isGoal = isGoal;
        }

        public PDMPOsearchResult(Domain.DomainValue action, AbstractDouble bestUtility) {
            this(action, bestUtility, false);
        }

        public PDMPOsearchResult(Domain.DomainValue action, AbstractDouble bestUtility, int time, boolean isGoal) {
            this(action, bestUtility, isGoal);
            this.time = time;
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

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public boolean isGoal() {
            return isGoal;
        }

        public void setGoal(boolean goal) {
            isGoal = goal;
        }

        @Override
        public String toString() {
            return "PDMPOsearchResult{" +
                    "action=" + action +
                    ", bestUtility=" + bestUtility +
                    ", isGoal=" + isGoal +
                    ", time=" + time +
                    '}';
        }
    }

    class ActionResult {

        private Domain.DomainValue action;

        private Distribution forwardPrevision;

        private AbstractDouble estimation;

        public ActionResult(Domain.DomainValue action, Distribution forwardPrevision, AbstractDouble estimation) {
            this.action = action;
            this.forwardPrevision = forwardPrevision;
            this.estimation = estimation;
        }

        public Domain.DomainValue getAction() {
            return action;
        }

        public void setAction(Domain.DomainValue action) {
            this.action = action;
        }

        public Distribution getForwardPrevision() {
            return forwardPrevision;
        }

        public void setForwardPrevision(Distribution forwardPrevision) {
            this.forwardPrevision = forwardPrevision;
        }

        public AbstractDouble getEstimation() {
            return estimation;
        }

        public void setEstimation(AbstractDouble estimation) {
            this.estimation = estimation;
        }

        @Override
        public String toString() {
            return "ActionResult{" +
                    "action=" + action +
                    ", estimation=" + estimation +
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
