package inference.exact;

import domain.Domain;
import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.Variable;

import java.util.*;

public class EnumerationAsk {


    private static String requestKey(List<Variable> variables){

        StringBuilder builder = new StringBuilder();

        for(Variable variable : variables){

            builder.append(variable.getDomainValue().toString());
        }

        return builder.toString();
    }

    private static List<List<Object>> requestValuesCombinaisons(List<Variable> variables){

        //premiere dimension le nombre de combinaison, deuxieme dimension le nombre de variables
        List<List<Object>> requestValuesCombinaisons = new LinkedList<>();

        requestValuesCombinaisons(requestValuesCombinaisons, new LinkedList<>(), variables, 0);

        return requestValuesCombinaisons;
    }

    private static void requestValuesCombinaisons(List<List<Object>> requestValuesCombinaisons , LinkedList<Object> varsValues, List<Variable> variables, int iVar){

        //si aucune variable restantes
        //on ajoute le tableau de valeurs pour chaque variables dans le même ordre que les variables de la requetes
        if(variables.size() == iVar){

            requestValuesCombinaisons.add(new ArrayList<>(varsValues));

            return;
        }

        Variable var = variables.get(iVar);

        for(Domain.DomainValue domainValue : var.getDomainValues()){

            varsValues.addLast(domainValue.getValue());

            requestValuesCombinaisons(requestValuesCombinaisons, varsValues, variables, iVar + 1);

            varsValues.removeLast();
        }
    }

    //request : une liste de preference ArrayList
    public static AbstractDouble ask(List<Variable> request, List<Variable> observations, BayesianNetwork network){

        BayesianNetwork.markImportantVars(request, observations);

        //Map liant une clé representant une combinaison de valeur pour les variable de la requete et une probabilité
        Hashtable<String, AbstractDouble> distribution = new Hashtable<>();
        //clé de combinaison de valeur de la requete d'origine
        String requestKey = requestKey(request);

        LinkedList<Variable> vars = network.getTopologicalOrder();

        List<List<Object>> requestValuesCombinaisons = requestValuesCombinaisons(request);

        //pour chaque combinaison de valeur
        for(List<Object> requestValues : requestValuesCombinaisons){

            for(int i = 0 ; i < requestValues.size() ; i ++){
                //initialise une variable de la requete
                request.get(i).setValue(requestValues.get(i));
            }

            String requestValuesKey = requestKey(request);

            distribution.put(requestValuesKey, enumerateAll(vars, network));
        }

        AbstractDouble total = network.getDoubleFactory().getNew(0.0);

        for(Map.Entry<String,AbstractDouble> entry : distribution.entrySet()){

            total = total.add(entry.getValue());
        }

        return distribution.get(requestKey).divide(total);

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
