package inference.approximation;

import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.Variable;

import java.util.List;

public class SimilaritySamplingAsk {

    public static AbstractDouble ask(List<Variable> request, List<Variable> obs, BayesianNetwork network, int maxSample) {

        network.markImportantVars(request, obs);

        List<Variable> variables = network.getTopologicalOrder();
        //retrait des observations qui ne seront pas echantillonées
        variables.removeAll(obs);

        //sauvegarde les valeurs originales des variables de requete et d'observation
        for (Variable reqVar : request) {

            reqVar.saveOriginValue();
        }

        AbstractDouble totalWeight = network.getDoubleFactory().getNew(0.0);

        AbstractDouble totalReqWeight = network.getDoubleFactory().getNew(0.0);

        for (int s = 0; s < maxSample; s++) {

            ///echantillonage les variables
            for (Variable var : variables) {

                var.initRdmValue();
            }

            AbstractDouble sampleWeight = network.getDoubleFactory().getNew(1.0);

            for (Variable o : obs) {
                //on pondere l'echantillon par sa vraissemblance
                sampleWeight = sampleWeight.multiply(o.getProbabilityForCurrentValue());
            }

            boolean nextSample = false;
            //ajoute le poids au poids total, çàd sans se soucier de la combinaison
            //de valeurs pour les variable de la requete
            totalWeight = totalWeight.add(sampleWeight);

            //et on test les variables de requete
            //même principe et on passe à l'echantillon suivant si une variable de requete ne correspond pas
            for (Variable req : request) {

                if (!req.originalValueMatch()) {

                    nextSample = true; break;
                }
            }

            if (nextSample) continue;

            //passer ce cap on ajoute le poids de l'echantillon à ceux correspndant à la requete
            totalReqWeight = totalReqWeight.add(sampleWeight);
        }

        System.out.println("MAX SAMPLE "+maxSample);
        System.out.println("TOTAL WEIGH "+totalWeight);
        System.out.println("REQ WEIGH "+totalReqWeight);

        return totalReqWeight.divide(totalWeight);
    }

}
