package inference.approximation;

import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.MarkovCoverDistributionCompute;
import network.Variable;

import java.util.List;

public class GibbsAsk {

    public static AbstractDouble ask(List<Variable> request, List<Variable> obs, BayesianNetwork network, int maxSample,
                                     MarkovCoverDistributionCompute markovCoverDistributionCompute) {

        network.markImportantVars(request, obs);

        List<Variable> variables = network.getTopologicalOrder();

        variables.removeAll(obs);

        for(Variable reqVar : request){

            reqVar.saveOriginValue();
        }

        for(Variable hiddenVar : variables){
            //initialise la maniere dont est calculé la distribution en focntion de la couverture de markov
            hiddenVar.setMarkovCoverDistributionCompute(markovCoverDistributionCompute);

            hiddenVar.initCumulativeMarkovFrequencies(obs);

            hiddenVar.initRdmValue();
        }

        double totalSampleReq = 0;

        for(int s = 0 ; s < maxSample ; s ++){

            for(Variable hiddenVar : variables){

                hiddenVar.initRandomValueFromMarkovCover();
            }

            boolean sampleOk = true;

            for(Variable reqVar : request){

                if(!reqVar.originalValueMatch()){

                    sampleOk = false;

                    break;
                }
            }

            if(sampleOk){

                totalSampleReq ++;
            }
        }

        System.out.println("MAX : "+maxSample);

        System.out.println("TOTAL : "+totalSampleReq);

        double rs = totalSampleReq / maxSample;

        return network.getDoubleFactory().getNew(rs);
    }
}
