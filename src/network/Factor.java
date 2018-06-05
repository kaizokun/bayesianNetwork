package network;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class Factor {

    private Set<Variable> vars = new LinkedHashSet<>();

    private Object[] matrix;

    private int varInit = 0;

    private AbstractDoubleFactory doubleFactory;


    /*
    *
    * !!! cas potentiellement problématiques :
    *
    * la liste de variable du facteur est nule, reste à deteminer un cas ou ça peut arriver mais,
    * Si on initialise à partir d'une variable on pourrait enregistrer la valeur de probabilité
    * et la retourner au lieu d'inspecter la matrice qui dans ce cas est nule.
    *
    * Pour le cas des facteurs crée à parti d'autre ça signifirait qu'i seriat créé à parti d'autre facteurs
    * contenant uniquement la variable à éliminer ...
    *
    * */

    public Factor(Variable var, AbstractDoubleFactory doubleFactory) {

        this.doubleFactory = doubleFactory;

        this.addVar(var);

        for (Variable variable : var.dependencies) {

            this.addVar(variable);
        }

        if (this.vars.isEmpty()) {

            //throw new Exception("impossible de créer un facteur sans variable non initialisées");
        }

        LinkedList<Variable> varList = new LinkedList<>(this.vars);

        this.matrix = new Object[varList.getFirst().getDomainSize()];

        this.initFactor(this.matrix, varList, var);

        //permet de savoir quel facteur sont lié a quel variable
        //afin d'enregistrer les facteurs à traiter lorsque suffisamment de variables
        //ont été initialisé
    }

    public Factor(Set<Variable> p_vars, AbstractDoubleFactory doubleFactory) {

        //System.out.println(p_vars.size());

        this.doubleFactory = doubleFactory;

        this.vars = p_vars;

        LinkedList<Variable> varList = new LinkedList<>(p_vars);

        this.matrix = new Object[varList.getFirst().getDomainSize()];

        this.initFactor(this.matrix, varList, null);
    }

    public static AbstractDouble getRequestDistribution(Map<Variable, Domain.DomainValue> reqValues, List<Variable> reqVars, Factor factor) {

        //reattribution des valeurs de la requete aux variables
        for(Variable reqVar : reqVars){

            reqVar.setDomainValue(reqValues.get(reqVar));
        }
        //valeur du facteur pour le svaleur de la requete
        AbstractDouble reqValue = factor.getValue();
        //total des valeurs du facteur
        AbstractDouble totalFactor = factor.totalFactor();

        return reqValue.divide(totalFactor);
    }

    private void addFactorToVar() {

        for (Variable variable : this.vars) {

            variable.addFactor(this);
        }
    }

    private void addVar(Variable variable) {

        if (!variable.isInit()) {

            this.vars.add(variable);
        }
    }

    private void initFactor(Object[] matrix, LinkedList<Variable> varList, Variable var) {

        //on pourrait aussi stocker les variables dans un arrayList et passer d'un indice à l'autre

        //récupere la première variable
        Variable variable = varList.removeFirst();

        int iVal = 0;
        //pour chaque valeur du domaine
        for (Domain.DomainValue value : variable.getDomainValues()) {
            //initialise la variable avec un de ses valeurs
            variable.setDomainValue(value);
            //si il ne reste plus de variable on a une assignation de valeur complete
            //et une probabilité que l'on enregistre dans la dimension de la matrice final
            if (varList.isEmpty()) {

                if (var != null) {

                    matrix[iVal] = var.getProbabilityForCurrentValue();

                } else {

                    matrix[iVal] = doubleFactory.getNew(0.0);
                }

            } else {
                //sinon on crée une nouvelle dimension
                Object[] newDimension = new Object[varList.getFirst().getDomainSize()];

                matrix[iVal] = newDimension;

                initFactor(newDimension, varList, var);
            }

            iVal++;
        }

        variable.clear();
        //ne serait pas utile avec un arraylist et le jeu d'indice
        varList.addFirst(variable);
    }

    public static void variableElimination(Set<Factor> factors, Variable var, boolean varReq, AbstractDoubleFactory doubleFactory) {

       // System.out.println("----------VAR ELIMIN----------------");
        //set de facteur à utiliser
        Set<Factor> factorsMul = new LinkedHashSet();

        //Factor newFactor = new Factor();
        Set<Variable> newFactorVars = new LinkedHashSet<>();

        //on enregistre les facteurs à multiplier et sommer
        //en on enregistre les variables communes dans le facteur courant
        for (Factor factor : factors) {

            if (factor.vars.contains(var)) {

                factorsMul.add(factor);

                newFactorVars.addAll(factor.vars);

                factor.addFactorToVar();
            }
        }

        //on retire la variable a supprimer de celles du facteur
        //sauf si il s'agit d'une variable de la requete
        if(!varReq) {

            newFactorVars.remove(var);
        }

        //System.out.println("new factor vars "+newFactorVars);

        Factor newFactor = new Factor(newFactorVars, doubleFactory);

        //création d'une liste de variables à traiter pour la multiplication et sommation
        List<Variable> vars = new ArrayList<>();
        //ajout de la variable à sommer en début de liste
        //inutile si il s'agit d'une variable de la requete dont il faut calculer le resultat
        //car elle fait parti des variables du facteur
        if(!varReq) {

            vars.add(var);
        }

        //suivit des variables du facteur finale
        vars.addAll(newFactor.vars);

        //les variables seront assignées dans l'ordre, pour chaque variable on verifie
        //les facteurs liées qui seront actifs au moment ou elle seront traités
        for (Variable variable : vars) {

            variable.checkActiveFactors();
        }

        sumMulFactors(newFactor, vars, doubleFactory.getNew(1.0), 0, doubleFactory);

        //on retire ces facteurs à multiplier de la liste d'origine
        factors.removeAll(factorsMul);
        //on retire les facteurs enregistrés dans les variables

        for (Variable variable : vars) {

            variable.clearFactors();
        }

        factors.add(newFactor);
    }

    private static void sumMulFactors(Factor newFactor, List<Variable> vars, AbstractDouble mul, int iVar,
                                      AbstractDoubleFactory doubleFactory) {

        //toutes les variables sont assignées
        if (iVar == vars.size()) {
            //ont addtionne les valeurs multipliés avec les precedentes
            newFactor.addValue(mul);

            return;
        }
        //récuperation de la première variable
        Variable variable = vars.get(iVar);

        //pour chaque valeur du domaine de la variable
        for (Domain.DomainValue value : variable.getDomainValues()) {

            //initialise la variable
            variable.setDomainValue(value);
            //sauvegarde la valeur de multiplication pour les valeurs de variables precedentes
            AbstractDouble mul2 = mul;
            //pour chaque facteur pouvant fournir un valeur arrivé à cette variable,
            //multiplier la valeur de l'entrée de la matrice pour une combinaison de valeur
            //System.out.println("Active factor "+variable.getActiveFactors().size());

            for (Factor factor : variable.getActiveFactors()) {

                mul2 = mul2.multiply(factor.getValue());
            }

            sumMulFactors(newFactor, vars, mul2, iVar + 1, doubleFactory);
        }

        variable.clear();
    }

    private DimensionId getDimensionId(){

        //récupere la premiere dimension
        Object[] dimension = this.matrix;

        int id;

        Iterator<Variable> variableIterator = this.vars.iterator();
        //on recupere le tableau de probabilités
        //correspondant à la combinaison de valeur des variables du facteur
        for (int iVar = 0; iVar < vars.size() - 1; iVar++) {

            Variable var = variableIterator.next();

            id = var.getValueId();

            dimension = (Object[]) dimension[id];
        }
        //la derniere dimension contient les valeurs
        Variable var = variableIterator.next();

        id = var.getValueId();

        return new DimensionId(dimension, id);
    }


    private class DimensionId{

        private Object[] dimension;

        private int id;

        public DimensionId(Object[] dimension, int id) {

            this.dimension = dimension;

            this.id = id;
        }
    }

    private AbstractDouble getValue() {

        DimensionId di = this.getDimensionId();

        return (AbstractDouble) di.dimension[di.id];
    }

    private void addValue(AbstractDouble valueAdd) {

        DimensionId di = this.getDimensionId();

        di.dimension[di.id] = ((AbstractDouble) di.dimension[di.id]).add(valueAdd);
    }


    private AbstractDouble totalFactor(){

        return totalFactor(this.matrix, doubleFactory.getNew(0.0));
    }

    private AbstractDouble totalFactor(Object[] dimension, AbstractDouble total){

        for (Object o : dimension) {

            if (o instanceof Object[]) {

                total = totalFactor((Object[]) o, total);

            } else {

                total = total.add(o);
            }
        }

        return total;
    }


    public void ctpVarInit(Variable var) {

        this.varInit++;

        if (this.varInit == this.vars.size()) {

            var.addActiveFactor(this);
        }
    }

    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder();

        loadString(this.matrix, stringBuilder);

        return stringBuilder.toString();
    }


    private void loadString(Object[] dimension, StringBuilder stringBuilder) {

        for (Object o : dimension) {

            if (o instanceof Object[]) {

                loadString((Object[]) o, stringBuilder);

            } else {

                stringBuilder.append(o);

                stringBuilder.append(",");
            }
        }
    }


}
