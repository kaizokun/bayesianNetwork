package inference.exact;

import domain.Domain;
import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.MegaVariable;
import network.Variable;

import java.util.*;

public class EnumerationAsk {

    //request : une liste de preference ArrayList
    public static AbstractDouble ask(List<Variable> request, List<Variable> observations, BayesianNetwork network){

        Variable megaRequest = request.size() == 1 ? request.get(0) : MegaVariable.encapsulate(request);

        BayesianNetwork.markImportantVars(request, observations);

        //Map liant une clé representant une combinaison de valeur pour les variable de la requete et une probabilité
        Hashtable<Domain.DomainValue, AbstractDouble> distribution = new Hashtable<>();
        //clé de combinaison de valeur de la requete d'origine
        Domain.DomainValue requestValue = megaRequest.getDomainValue();

        LinkedList<Variable> vars = network.getTopologicalOrder();

        //pour chaque combinaison de valeur
        for(Domain.DomainValue value : megaRequest.getDomainValues()){

            megaRequest.setDomainValue(value);

            distribution.put(value, enumerateAll(vars, network));
        }

        AbstractDouble total = network.getDoubleFactory().getNew(0.0);

        for(Map.Entry<Domain.DomainValue,AbstractDouble> entry : distribution.entrySet()){

            total = total.add(entry.getValue());
        }

        return distribution.get(requestValue).divide(total);

    }

    private static AbstractDouble enumerateAll(LinkedList<Variable> vars, BayesianNetwork network) {

        if( vars.isEmpty() ){

            return network.getDoubleFactory().getNew(1.0);
        }

        Variable var = vars.removeFirst();

        if( var.isInit() ){

            AbstractDouble rs = var.getProbabilityForCurrentValue().multiply(enumerateAll(vars, network));

            vars.addFirst(var);

            return rs;

        }else {

            AbstractDouble sum = network.getDoubleFactory().getNew(0.0);

            for (Domain.DomainValue value : var.getDomain().getValues()) {

                var.setDomainValue(value);

                sum = sum.add(var.getProbabilityForCurrentValue().multiply(enumerateAll(vars, network)));
            }

            var.clear();

            vars.addFirst(var);

            return sum;

        }

    }

}
