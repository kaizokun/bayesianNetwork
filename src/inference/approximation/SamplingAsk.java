package inference.approximation;

import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import network.BayesianNetwork;
import network.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SamplingAsk {

    public static AbstractDouble ask(List<Variable> request, List<Variable> obs, BayesianNetwork network, int maxSample) {

/*

        récuperer les variables importantes dans l'ordre topologique dans un tableau

        pour chaque variable dans l'ordre topologique
            enregistrer le numero d'ordre dans la variable
         Fin pour

        sauvegarder des copies des variables de la requete
        sauvegarder des copies des variables d'observations
        (initialisées avec uniquement la valeur et l'indice temporaire)

        totalSampleMatchObservation <- 0
        totalSampleMatchObservationAndRequest <- 0

        pour un certain nombre d' échantillons

            pour chaque variable dans l'ordre topologique

                echantilloner une valeur pour la variable et lui assigner

            Fin pour

            totalMatch <- 0;

            Pour chaque variable d'observation
                recuperer l'indice de la variable
                et comparer sa valeur à la valeur echantilloné
                Si correspondance  inrementer total match
            Fin Pour

            si totalMatch est égal au nombre de variable d'observation
            compter un echantillon correspondant
                incrementer totalSampleMatchObservation
            fin si

           totalMatch <- 0;

           Pour chaque variable de requete
                recuperer l'indice de la variable
                et comparer sa valeur à la valeur echantilloné
                Si correspondance  inrementer total match
            Fin Pour

            si totalMatch est égal au nombre de variable de requete
            compter un echantillon correspondant
                incrementer totalSampleMatchObservationAndRequest
            fin si

        fin pour


*/

        network.markImportantVars(request, obs);

        List<Variable> variables = network.getTopologicalOrder();

        List<Variable> variablesTab = new ArrayList<>(Arrays.asList(new Variable[variables.size()]));

        int i = 0;

        for(Variable variable : variables){

            variable.setTempIndex(i);

            variablesTab.set(i, variable);

            i++;
        }

        List<Variable> saveReq = new ArrayList<>();

        List<Variable> saveObs = new ArrayList<>();

        for(Variable reqVar : request){

            saveReq.add(reqVar.sampleCopy());
        }

        for(Variable obsVar : obs){

            saveObs.add(obsVar.sampleCopy());
        }

        double totalMatchSamplesObs = 0;

        double totalMatchSamplesObsReq = 0;

        for(int s = 0 ; s < maxSample ; s ++){

            for(Variable var : variablesTab){

                var.initRdmValue();
            }

            int totalMatchVars = 0;

            for(Variable o : saveObs){

                if(variablesTab.get(o.getTempIndex()).getValue().equals(o.getValue())){

                    totalMatchVars ++;
                }
            }

            if(totalMatchVars == saveObs.size()){

                totalMatchSamplesObs ++;

                totalMatchVars = 0;

                for(Variable req : saveReq){

                    if(variablesTab.get(req.getTempIndex()).getValue().equals(req.getValue())){

                        totalMatchVars ++;
                    }
                }

                if(totalMatchVars == saveReq.size()){

                    totalMatchSamplesObsReq ++;
                }
            }
        }

        AbstractDouble num = network.getDoubleFactory().getNew(totalMatchSamplesObsReq);

        AbstractDouble denom = network.getDoubleFactory().getNew(totalMatchSamplesObs);

        System.out.println(totalMatchSamplesObsReq+" / "+totalMatchSamplesObs);

        return num.divide(denom);
    }

}
