package inference.approximation;

import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.Variable;

import java.util.List;

public class GibbsAsk {

    public static AbstractDouble ask(List<Variable> request, List<Variable> obs, BayesianNetwork network, int maxSample) {

        network.markImportantVars(request, obs);

        List<Variable> variables = network.getTopologicalOrder();

        variables.removeAll(obs);

        for(Variable reqVar : request){

            reqVar.saveOriginValue();
        }

        for(Variable hiddenVar : variables){

            hiddenVar.loadMarkovCover(obs);

            hiddenVar.initCumulativeMarkovFrequencies();

            hiddenVar.initRdmValue();
        }

        double totalSampleReq = 0;

        for(int s = 0 ; s < maxSample ; s ++){

            for(Variable hiddenVar : variables){

                hiddenVar.initValueFromMarkovCover();
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
