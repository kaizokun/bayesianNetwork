package inference.dynamic;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import domain.data.MyDoubleFactory;
import network.Variable;

import java.util.*;

import static network.BayesianNetwork.requestValuesCombinations;

public class Util {


    protected static int maxDepth = Integer.MIN_VALUE;

    protected static String TOTAL_VALUE = "total";

    protected static Domain.DomainValue totalDomainValues = new Domain.DomainValue(TOTAL_VALUE);

    protected static Comparator<Variable> comparatorVarTimeLabel;

    static {

        comparatorVarTimeLabel = new Comparator<Variable>() {

            @Override
            public int compare(Variable v1, Variable v2) {

                int cmp = Integer.compare(v1.getTime(), v2.getTime());

                if (cmp == 0) {

                    return v1.getLabel().compareTo(v2.getLabel());
                }

                return cmp;
            }
        };
    }

    public static void showDistributions(String title, Map<String, Map<Domain.DomainValue, AbstractDouble>> distributions) {

        System.out.println();
        System.out.println(title + " " + distributions.size());
        System.out.println();

        for (Map.Entry<String, Map<Domain.DomainValue, AbstractDouble>> entry : distributions.entrySet()) {

            System.out.println(entry.getKey() + " " + entry.getValue());
        }
    }



    public static void showDynamicDistributions(Map<String, Map<Domain.DomainValue, AbstractDouble>> dynamic) {

        for (String key : dynamic.keySet()) {

            System.out.println("-----" + key + "-----");

            System.out.println("BASIC  : " + dynamic.get(key));

            System.out.println("NORMAL : " + normalizeDistribution(dynamic.get(key)));

            System.out.println();
        }
    }


    public static Map<Domain.DomainValue, AbstractDouble> getCombinatedDistribution(List<Variable> requests, MyDoubleFactory doubleFactory,
                                                                              List<Map<Domain.DomainValue, AbstractDouble>> distributions) {

        //Il faut récuperer chaque combinaison de valeur pour chaque variable de la requete
        List<List<Domain.DomainValue>> requestValuesList = requestValuesCombinations(requests);

        Map<Domain.DomainValue, AbstractDouble> finalDistribution = new Hashtable<>();

        AbstractDouble total = doubleFactory.getNew(0.0);

        //pour chaque combinaison de valeurs
        for (List<Domain.DomainValue> domainValues : requestValuesList) {

            AbstractDouble totalCombination = doubleFactory.getNew(1.0);

            int iRequest = 0;
            //pour chaque valeur de variable dans la combinaison
            for (Domain.DomainValue domainValue : domainValues) {

                //on récupère la probabilité correspondant à la valeur dans la distribution de la variable
                AbstractDouble varValueProb = distributions.get(iRequest).get(domainValue);
                //que l'on multiplie avec celle de la variable de requete precedente
                totalCombination = totalCombination.multiply(varValueProb);

                /*
                AbstractDouble varValueProbNormalised = varValueProb.divide(distributions.get(iVar).get(new Domain.DomainValue(TOTAL_VALUE)));

                totalCombination = totalCombination.multiply(varValueProbNormalised);
                */
                iRequest++;
            }

            finalDistribution.put(new Domain.DomainValue(domainValues), totalCombination);

            total = total.add(totalCombination);
        }


        /*
        if(requests.size() == 1) {
            //cas particulier ou la requete ne contient qu'une variable
            //normalement il faudrait eviter d'appeller cette fonction dans ce cas
            finalDistribution.put(new Domain.DomainValue(TOTAL_VALUE), distributions.get(0).get(totalDomainValues));

        }else{
*/
        finalDistribution.put(new Domain.DomainValue(TOTAL_VALUE), total);

        //}

        return finalDistribution;
    }


    public static Map<Domain.DomainValue, AbstractDouble> multiplyDistributions(AbstractDoubleFactory doubleFactory, Map<Domain.DomainValue, AbstractDouble>... distributions) {

        //distribution final issue des distributions d'origine
        //et dont les valerus ont été multipliées pour chaque entrées qui sont identiques dans les tables
        Map<Domain.DomainValue, AbstractDouble> finalDistribution = new Hashtable<>();
        //pour chaque clé de la table : un Object DomainValue ( qui peut être composé d'une liste de DomainValue
        //cas de plusieurs variable par requete )
        for (Domain.DomainValue domainValue : distributions[0].keySet()) {

            if (domainValue.equals(totalDomainValues)) continue;

            AbstractDouble valueTotal = doubleFactory.getNew(1.0);
            //multiplie les entrées pour chaque clé une par une dans chaque distribution
            for (Map<Domain.DomainValue, AbstractDouble> distribution : distributions) {

                valueTotal = valueTotal.multiply(distribution.get(domainValue));
            }

            finalDistribution.put(domainValue, valueTotal);
        }

        return finalDistribution;
    }

    public static Map<Domain.DomainValue, AbstractDouble> normalizeDistribution(Map<Domain.DomainValue, AbstractDouble> distrib) {

        Map<Domain.DomainValue, AbstractDouble> distribNormalized = new Hashtable<>();

        for (Domain.DomainValue domainValue : distrib.keySet()) {

            distribNormalized.put(domainValue, distrib.get(domainValue).divide(distrib.get(totalDomainValues)));
        }

        return distribNormalized;
    }

    public static void addTotalToDistribution(AbstractDoubleFactory doubleFactory, Map<Domain.DomainValue, AbstractDouble> distributionFinal) {

        AbstractDouble total = doubleFactory.getNew(0.0);

        for (AbstractDouble value : distributionFinal.values()) {

            total = total.add(value);
        }

        distributionFinal.put(totalDomainValues, total);
    }


    public static String getDistribSavedKey(Collection<Variable> variables, int time) {

        StringBuilder keybuilder = new StringBuilder();

        for (Variable variable : variables) {

            keybuilder.append(variable.getLabel() + "_" + time);

            keybuilder.append('.');
        }

        keybuilder.deleteCharAt(keybuilder.length() - 1);

        return keybuilder.toString();
    }

    public static String getDistribSavedKey(Collection<Variable> variables) {

        StringBuilder keybuilder = new StringBuilder();

        for (Variable variable : variables) {

            keybuilder.append(variable.getLabel() + "_" + variable.getTime());

            keybuilder.append('.');
        }

        keybuilder.deleteCharAt(keybuilder.length() - 1);

        return keybuilder.toString();
    }

    public static String getDistribSavedKey(Variable variable) {

        return getDistribSavedKey(variable, variable.getTime());
    }

    public static String getDistribSavedKey(Variable variable, int time) {

        return variable.getLabel() + "_" + time;
    }

    public static  String getTreeIdent(int depth) {

        StringBuilder ident = new StringBuilder();

        for (int i = 0; i < depth - 1; i++) {

            ident.append("             ");
        }

        if (depth > 0) {

            ident.append("[____________");
        }

        return ident.toString();
    }

    public static  String getIdent(int depth) {

        StringBuilder ident = new StringBuilder();

        for (int i = 0; i < depth; i++) {

            ident.append("             ");
        }
        return ident.toString();
    }

    public static List<Domain.DomainValue> getDomainValues(Collection<Variable> variables){

        List<Domain.DomainValue> domainValues = new LinkedList<>();

        for (Variable variable : variables) {

            domainValues.add(variable.getDomainValue());
        }

        return domainValues;
    }

    public static void resetDomainValues(Collection<Variable> variables, Collection<Domain.DomainValue> domainValues){

        Iterator<Variable> variableIterator = variables.iterator();

        for (Domain.DomainValue domainValue : domainValues) {

            variableIterator.next().setDomainValue(domainValue);
        }
    }

}
