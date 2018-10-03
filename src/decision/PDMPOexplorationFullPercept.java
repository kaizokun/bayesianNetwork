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


public class PDMPOexplorationFullPercept extends PDMPOexploration {

    private AbstractDouble maxPerceptProb, minPerceptProb;

    public PDMPOexplorationFullPercept(AbstractDoubleFactory doubleFactory,
                                       DynamicBayesianNetwork dynamicBayesianNetwork,
                                       PDMPO pdmpo,
                                       double minStateProb,
                                       double maxPerceptProb) {

        super(doubleFactory, dynamicBayesianNetwork, pdmpo, minStateProb);

        this.maxPerceptProb = df.getNew(maxPerceptProb);
    }

    /**
     * @param doubleFactory : fabrique de double ou bigdecimal
     * @param minStateProb : probabilité minimum en deça de laquelle un état est ignoré
     * @param  maxPerceptProb : seul de probabilité cumulé à atteindre pour les percepts
     * @param minPerceptProb : probabilité minimum en deça de laquelle un percept est ignoré
     * */
    public PDMPOexplorationFullPercept(AbstractDoubleFactory doubleFactory, double minStateProb, double maxPerceptProb,
                                       double minPerceptProb, double minRiskProb) {

        super(doubleFactory, minStateProb, minRiskProb);

        this.maxPerceptProb = doubleFactory.getNew(maxPerceptProb);

        this.minPerceptProb = doubleFactory.getNew(minPerceptProb);
    }

    @Override
    protected List<Map.Entry<Domain.DomainValue, AbstractDouble>> filterPercepts(
            Map<Domain.DomainValue, AbstractDouble> perceptsMap, String ident) {

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

        //System.out.println(ident+" Percepts sorted "+perceptsTab);

        //indice de fin exclusif du sous tableau de percepts
        int maxIndex = 0;

        AbstractDouble totalProb = df.getNew(0.0);

        for (Map.Entry<Domain.DomainValue, AbstractDouble> perceptProb : perceptsTab) {

            //si probabilité du percept supérieur ou égal à la limite fixé on en tiens compte
            //réduit les percepts
            if(perceptProb.getValue().compareTo(minPerceptProb) >= 0) {

                //ajoute la probabilité des percepts dans l'ordre decroissant
                totalProb = totalProb.add(perceptProb.getValue());

                maxIndex++;
                //si on atteind le seuil limite on arrete
                if (totalProb.compareTo(maxPerceptProb) >= 0) break;

            }else{
                //si inférieur on arrete étant donné que les percpets sont triés par ordre decroissant de probabilité
                break;
            }
        }
        /*
        System.out.println();
        System.out.println("----------------------------");
        System.out.println("percepts");

        System.out.println(perceptsTab);

        System.out.println(perceptsTab.subList(0, maxIndex));
*/
        //System.out.println(ident+" Percepts sorted sublist before normalisation : "+perceptsTab.subList(0, maxIndex));

        //normaliser les probabilités de la sous liste pour obtenir un total de 100 %
        for (Map.Entry<Domain.DomainValue, AbstractDouble> percept : perceptsTab.subList(0, maxIndex)) {

            percept.setValue(percept.getValue().divide(totalProb));
        }

        //System.out.println(ident+" Percepts sorted sublist normalise : "+perceptsTab.subList(0, maxIndex));


        return perceptsTab.subList(0, maxIndex);
    }

    public void setMaxPerceptProb(AbstractDouble maxPerceptProb) {
        this.maxPerceptProb = maxPerceptProb;
    }
}
