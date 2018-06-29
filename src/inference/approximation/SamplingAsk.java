package inference.approximation;

import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.Variable;

import java.util.List;

public class SamplingAsk {

    public static AbstractDouble ask(List<Variable> request, List<Variable> obs, BayesianNetwork network, int maxSample) {

        network.markImportantVars(request, obs);

        List<Variable> variables = network.getTopologicalOrder();
        //sauvegarde les valeurs originales des colVars de requete et d'isObservation
        for (Variable reqVar : request) {

            reqVar.saveOriginValue();
        }

        for (Variable obsVar : obs) {

            obsVar.saveOriginValue();
        }

        double totalMatchSamplesObs = 0;

        double totalMatchSamplesObsReq = 0;

        for (int s = 0; s < maxSample; s++) {

            ///echantillonage des colVars
            for (Variable var : variables) {

                var.initRdmValue();
            }

            //passe au prochain sample
            boolean nextSample = false;

            for (Variable o : obs) {
                //si la valeur de la variable d'isObservation ne correspond plus
                // on passe au prochain echantillon
                if (!o.originalValueMatch()) {

                    nextSample = true; break;
                }
            }

            if (nextSample) continue;

            //si on passe se cap on compte un echantillon valide
            //et on test les colVars de requete

            totalMatchSamplesObs++;
            //même principe on passe à l'echantillon suivant si une variable de requete ne correspond pas
            for (Variable req : request) {

                if (!req.originalValueMatch()) {

                    nextSample = true; break;
                }
            }

            if (nextSample) continue;

            totalMatchSamplesObsReq++;

        }

        System.out.println("MAX SAMPLE "+maxSample);
        System.out.println("OBS SAMPLE "+totalMatchSamplesObs);
        System.out.println("REQ SAMPLE "+totalMatchSamplesObsReq);

        AbstractDouble num = network.getDoubleFactory().getNew(totalMatchSamplesObsReq);

        AbstractDouble denom = network.getDoubleFactory().getNew(totalMatchSamplesObs);

        return num.divide(denom);
    }

}
