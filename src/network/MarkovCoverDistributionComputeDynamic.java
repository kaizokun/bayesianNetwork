package network;

import domain.Domain;
import domain.data.AbstractDouble;

import java.util.*;

public class MarkovCoverDistributionComputeDynamic implements MarkovCoverDistributionCompute {

    @Override
    public void initCumulativeMarkovFrequencies(List<Variable> obs, Variable variable) { }

    @Override
    public Domain.DomainValue getRandomValueFromMarkovCover(Variable variable) {

        //total de la distribution de la variable en fonction de sa couverture de markov
        AbstractDouble distribTotal = variable.doubleFactory().getNew(0.0);
        //distribution
        List<Map.Entry<Domain.DomainValue, AbstractDouble>> markovProbs = new LinkedList<>();
        //calcul de la distribution markovienne pour chaque valeur
        for(Domain.DomainValue domainValue : variable.getDomainValues()){

            variable.setDomainValue(domainValue);

            AbstractDouble markovProb = variable.doubleFactory().getNew(1.0);

            markovProb = markovProb.multiply(variable.getProbability());

            for(Variable child : variable.children){

                markovProb = markovProb.multiply(child.getProbability());
            }

            distribTotal = distribTotal.add(markovProb);

            markovProbs.add(new AbstractMap.SimpleEntry(domainValue, markovProb));
        }

        //distribution aléatoire
        AbstractDouble rdm = variable.doubleFactory().getNew(new Random().nextDouble());

        AbstractDouble max = variable.doubleFactory().getNew(0.0);

        for(Map.Entry<Domain.DomainValue, AbstractDouble> markovProb : markovProbs){

            //divise la probabilité par le total pour avoir une distribution total sur 100%
            //et l'additionne à la précédente pour cumuler jusqu'à atteindre 100% (0.999...9)
            max = max.add(markovProb.getValue().divide(distribTotal));

            if( rdm.compareTo(max) < 0 ){

                return markovProb.getKey();
            }
        }
        //pas censé se produire sinon retourne la deniere valeur
        return markovProbs.get(markovProbs.size() - 1).getKey();
    }
}
