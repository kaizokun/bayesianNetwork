package inference.exact;

import domain.Domain;
import domain.data.AbstractDouble;
import network.BayesianNetwork;
import network.Factor;
import network.Variable;
import util.ListUtil;

import java.util.*;

public class EliminationAsk {

    public static AbstractDouble ask(List<Variable> request, List<Variable> observations, BayesianNetwork bayesianNetwork) {

        BayesianNetwork.markImportantVars(request, observations);

        //copie des colVars de la requete et de leur valeur d'origine
        //reset des valeurs de la requetes seul les isObservation seront ignorées
        Map<Variable, Domain.DomainValue> reqValues = new Hashtable<>();

        for(Variable varReq : request){

            reqValues.put(varReq,varReq.getDomainValue());

            varReq.clear();
        }

        Set<Factor> factorSet = new LinkedHashSet<>();
        //set de variable de la requete permettant de voir efficacement si une variable fait parti de la requete
        //on pourrait aussi ajouter un attribut bolleen dans Variable permettent de le savoir
        Set<Variable> requestVars = new LinkedHashSet<>(request);

        LinkedList<Variable> vars = bayesianNetwork.getTopologicalOrder();
        //ordre topologique inversé pour traiter les colVars les plus bas niveau en premier
        vars = ListUtil.reverseOrder(vars);

        for (Variable var : vars) {

            Factor factor = new Factor(var, bayesianNetwork.getDoubleFactory());

            factorSet.add(factor);

            if (!var.isInit()) {

                Factor.variableElimination(factorSet, var, requestVars.contains(var), bayesianNetwork.getDoubleFactory());
            }
        }

        //logiquement un seul facteur doit demeurer, à verifier...
        Factor rsFactor = factorSet.iterator().next();

        return Factor.getRequestDistribution(reqValues, request, rsFactor);

    }

}
