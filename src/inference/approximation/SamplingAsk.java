package inference.approximation;

import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SamplingAsk {

    public static AbstractDouble ask(List<Variable> request, List<Variable> obs, BayesianNetwork network, int maxSample) {

        network.markImportantVars(request, obs);

        List<Variable> variables = network.getTopologicalOrder();
        //sauvegarde les valeurs originales des variables de requete et d'observation
        for (Variable reqVar : request) {

            reqVar.saveOriginValue();
        }

        for (Variable obsVar : obs) {

            obsVar.saveOriginValue();
        }

        double totalMatchSamplesObs = 0;

        double totalMatchSamplesObsReq = 0;

        for (int s = 0; s < maxSample; s++) {

            ///echantillonage des variables
            for (Variable var : variables) {

                var.initRdmValue();
            }

            //passe au prochain sample
            boolean nextSample = false;

            for (Variable o : obs) {
                //si la valeur de la variable d'observation ne correspond plus
                // on passe au prochain echantillon
                if (!o.originalValueMatch()) {

                    nextSample = true; break;
                }
            }

            if (nextSample) continue;

            //si on passe se cap on compte un echantillon valide
            //et on test les variables de requete

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

        AbstractDouble num = network.getDoubleFactory().getNew(totalMatchSamplesObsReq);

        AbstractDouble denom = network.getDoubleFactory().getNew(totalMatchSamplesObs);

        return num.divide(denom);
    }

}
