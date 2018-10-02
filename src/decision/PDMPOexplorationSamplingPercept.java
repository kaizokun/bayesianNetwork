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


public class PDMPOexplorationSamplingPercept extends PDMPOexploration {

    /*
     * DynamicBayesianNetwork network : nouveau reseau bayesien ou dumoins réinitialisé à zero pour exploration
     * Distribution forward : état de croyance courant sur les états dans le RBD "réel"
     * */
/*
    public PDMPOexplorationSamplingPercept(AbstractDoubleFactory doubleFactory,
                                           DynamicBayesianNetwork dynamicBayesianNetwork,
                                           PDMPO pdmpo, double minStateProb) {

        super(doubleFactory, dynamicBayesianNetwork, pdmpo, minStateProb);
    }
*/

    public PDMPOexplorationSamplingPercept(AbstractDoubleFactory doubleFactory, double minStateProb) {
        super(doubleFactory, minStateProb);
    }

    @Override
    protected List<Map.Entry<Domain.DomainValue, AbstractDouble>> filterPercepts(
            Map<Domain.DomainValue, AbstractDouble> perceptsMap, String ident) {

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
        //met la limite max à 100%, dans certain cas elle est un peu en de dessous et une nombre aléatoire hors
        //limite peut etre généré
        frequencies.get(frequencies.size() - 1).getValue().setMax(network.getDoubleFactory().getNew(1.0));

        double rdmDouble = rdm.nextDouble();

        // System.out.println(ident + " " + rdmDouble);

        // System.out.println(ident + " " + frequencies);

        Domain.DomainValue samplePercept = FrequencyRange.dichotomicSearch(frequencies, network.getDoubleFactory().getNew(rdmDouble));

        return Arrays.asList(new AbstractMap.SimpleEntry(samplePercept, perceptsMap.get(samplePercept)));
    }


}
