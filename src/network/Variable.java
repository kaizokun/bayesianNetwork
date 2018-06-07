package network;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;
import network.ProbabilityComputeFromTCP.FrequencyRange;

import java.util.*;

public class Variable {

    protected int depth;

    protected IDomain domain;

    protected String label;

    protected Domain.DomainValue value, originValue;

    protected List<Variable> dependencies;

    protected List<Variable> children;

    protected ProbabilityCompute probabilityCompute;

    protected Hashtable<String, List<Map.Entry<Domain.DomainValue, FrequencyRange>>> cumulativeMarkovFrequencies;

    //utilisé dans la recuperation des noeuds dans l'odre topologique
    //pour savoir si ils ont été ajoutés, pas vraiment une propriété mais plus pratique que de créer
    //des tables de hachages à chaque.
    protected boolean added, important, isObs;

    //facteur actif une fois arrivé à cette variable dans la sous procédure
    // de sommation de l'algorithme d'elimination de variables
    //peut etre pas ca place ici car lié à un algorithme specifique
    //un table Map pourrait aussi faire l'affaire mais moins efficace car necessite une recherche
    //ici la relation et immédiate
    protected List<Factor> activeFactors = new LinkedList<>();
    //facteurs lié à cette variable
    protected List<Factor> factors = new LinkedList<>();

    public Variable() {
    }

    public Variable(String label, IDomain domain, ProbabilityCompute probabilityCompute) {

        this(label, domain, probabilityCompute, new ArrayList<>());
    }

    public Variable(String label, IDomain domain, ProbabilityCompute probabilityCompute, List<Variable> dependencies) {

        this.label = label;

        this.domain = domain;

        this.dependencies = dependencies;

        this.probabilityCompute = probabilityCompute;

        this.children = new ArrayList<>();

        for (Variable parent : dependencies) {

            parent.addChild(this);
        }

    }

    public Variable(Variable variable) {

        this.value = variable.value;

        this.label = variable.label;
    }

    private void addChild(Variable child) {

        this.children.add(child);
    }

    public Domain.DomainValue getDomainValue() {

        return value;
    }

    public Object getValue() {

        return value.getValue();
    }

    public Domain.DomainValue getOriginValue() {
        return originValue;
    }

    public void setOriginValue(Domain.DomainValue originValue) {
        this.originValue = originValue;
    }

    public void saveOriginValue() {

        this.setOriginValue(this.getDomainValue());
    }

    public int getValueId() {

        return this.value.getIndex();
    }

    public void setDomainValue(Domain.DomainValue value) {

        this.value = value;
    }

    public void setValue(Object value) {

        this.setDomainValue(this.domain.getDomainValue(value));
    }

    public boolean originalValueMatch() {

        return this.originValue.equals(this.getDomainValue());
    }

    public void checkActiveFactors() {

        for (Factor factor : factors) {

            factor.ctpVarInit(this);
        }
    }

    public boolean isInit() {
        return this.value != null;
    }

    public void clear() {
        this.value = null;
    }

    public List<Variable> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Variable> dependencies) {
        this.dependencies = dependencies;
    }

    public AbstractDouble getProbabilityForCurrentValue() {

        return probabilityCompute.getProbability(this);
    }


    public void initCumulativeMarkovFrequencies() {

        /*
         * Il faut ici genere une distribution sur la variable en fonction de sa couverture de markov
         * ensuite recuperer une valeur aléatoirement en focntion d'un nombre aléatoire de la même manière que la procédure initRdmValue
         * la difference est que la distribution ne se fait pas uniquement sur les parents mais aussi sur les enfants qui dependent également d'autres parents
         * possibilité de calculer et l'avance cette distribution en envisageant pour chaque combinaison de valeur parents associés à chaque combinaison de valeur enfant
         * dependants elles mêmes des combinaison de valeurs de leur parents ...
         *
         * initialiser une liste de variables LV avec la variable courante suivit de ses variables enfants
         *
         * initialiser un valeur double à 1 PROB la probabilité selon la couverture de Markov de la variable courante
         *
         * initialiser une liste de clé KeyList pour chaque combinaisn de valeur parent
         *
         * Fct-generer-markov-combinaison LV,KeyList, 1, 0
         *
         * */

        this.cumulativeMarkovFrequencies = new Hashtable<>();

        List<Variable> variables = new ArrayList<>(1 + this.children.size());

        variables.add(this);

        variables.addAll(this.children);

        AbstractDouble prob = this.probabilityCompute.getDoubleFactory().getNew(1.0);

        this.initCumulativeMarkovFrequencies(variables, new LinkedList(), prob, 0);

    }

    private void initCumulativeMarkovFrequencies(List<Variable> variables, LinkedList<String> keys, AbstractDouble prob, int iVar) {

        /*
         *   Si iVar = LV.taille
         *
         *
         *       créer une clé à partir de KeyList
         *
         *       concatener cette clé avec la combinaison des valeurs des variables enfants de la variable
         *
         *       pour obtenir la clé de premiere dimension
         *
         *       enregistrer pour la valeur courante de la variable  qui est la clé de deuxieme dimension
         *
         *       la valeur courante de PROB
         *
         *
         *
         *   Fin Si
         *
         *   Si iVar = LV.taille
         *
         *
         *      Récuperer la premier clé qui correspond à la combinaison de parents
         *
         *         <! un état pour la combinaison de markov dela variable comprend :
         *         une combinaison de valeur pour les parents de la variable
         *         une combinaison de valeur pour les variables enfants
         *         une combinaison de valeur pour les autres parents des variables enfants !>
         *
         *      Pour chaque clé qui suit qui est une combinaison de parents pour un enfant de la variable
         *
         *          <! cette clé permet d'identifier un état identique à l'exception de la variable courante
         *          que l'on pourrait retirer!>
         *
         *          spliter cette clé pour récuperer les valeurs parents sous forme de chaine
         *          verifier que la valeur de la variable à echantilloner à la bonne valeur
         *          dans cette clé, soit celle assigné à cet instant.
         *
         *          si ce n'est pas le cas retourner en arriere
         *
         *      Fin Pour
         *
         *
         *
         *
         *
         *   Fin Si
         *
         */

        if (iVar == variables.size()) {

            StringBuilder builder = new StringBuilder();
            //genere une clé à partir de la combinaison de valeurs parents courantes
            //et des combinaisons de valeurs parents pour les variables enfants

            //!! certaines entrées peuvent ne pas être cohérentes par exemple
            // si les mêmes variables ont des valeurs différentes dans des combinaison de valeurs parents
            //pour différentes variables enfants. ca ne pose pas de probleme lors de l'echantillonage car
            //les echantilons eux sont toujours coherents.
            for (String key : keys) {

                builder.append(key);

                builder.append('-');
            }

            for(Variable child : this.children){

                builder.append(child.getLabel()+"-");

                builder.append(child.getValue());

                builder.append('-');
            }

            //retire le tiret...
            if (builder.length() != 0) {

                builder.deleteCharAt(builder.length() - 1);
            }

            String key = builder.toString();
            //récupère la liste des probabilités cumulées pour cette combinaison
            List<Map.Entry<Domain.DomainValue, FrequencyRange>> cumulatedFreqList = this.cumulativeMarkovFrequencies.get(key);
            //crée la liste si necessaire
            if(cumulatedFreqList == null){

                cumulatedFreqList = new ArrayList<>();

                this.cumulativeMarkovFrequencies.put(key, cumulatedFreqList);
            }

            //valeur minimum du range min du range
            AbstractDouble  min = this.probabilityCompute.getDoubleFactory().getNew(0.0);

            if(!cumulatedFreqList.isEmpty()){
                //correspond au maximum du range de la valeur precedente si liste non vide
               // min = cumulatedFreqList.get(cumulatedFreqList.size() - 1).getValue().getMax();
            }else{
               //sinon demarre de zéro
              // min = this.probabilityCompute.getDoubleFactory().getNew(0.0);
            }
            //initialise le range avec le minimum et le max auquel
            //on ajoute la prob pour la valeur actuelle de la variable
            FrequencyRange frequencyRange = new FrequencyRange(min, min.add(prob));

            cumulatedFreqList.add(new AbstractMap.SimpleEntry(this.getDomainValue(), frequencyRange));

            return;
        }

        /*
         *   variable <- LV[iVar]

         *   Pour chaque combinaison de parent de variable soit chaque clé de la TCP
         *
         *       ajouter la clé de TCP à keyList
         *
         *       Pour chaque valeur du domaine de la variable
         *
         *           initialiser la variable avec cette valeur
         *
         *           PROB = PROB * probabilité de variable selon la combinaison de valeurs parents et sa valeur
         *
         *           Fct-generer markov combinaison LV, KeyList, PROB, iVar + 1
         *
         *       Fin Pour
         *
         *   Fin Pour
         *
         *
         * */

        Variable variable = variables.get(iVar);

        for(String parentKey : variable.probabilityCompute.getTCP().keySet()){

            keys.addLast(variable.getLabel()+" : "+parentKey);

            Hashtable<Domain.DomainValue,AbstractDouble> varProbs = variable.probabilityCompute.getTCP().get(parentKey);

            for(Domain.DomainValue domainValue : domain.getValues()){

                //initialise pour récuperer la valeur à la fin
                variable.setDomainValue(domainValue);
                //copie la valeur de la probabilité courante
                //probabilité de la variable courante en fonction d'une combi de valeur parents
                AbstractDouble prob2 = variable.probabilityCompute.getDoubleFactory().getNew(0.0).add(prob);

                prob2 = prob2.multiply(varProbs.get(domainValue));

                initCumulativeMarkovFrequencies(variables, keys, prob2, iVar + 1);
            }

            keys.removeLast();
        }
    }

    public void showCumulativeMarkovFrequencies(){

        for(Map.Entry<String,List<Map.Entry<Domain.DomainValue,FrequencyRange>>> entry : this.cumulativeMarkovFrequencies.entrySet()){

            System.out.println("=====KEY====");

            System.out.println(entry.getKey());

            System.out.println("=====VALUES====");

            for(Map.Entry<Domain.DomainValue,FrequencyRange> entry2 : entry.getValue()){

                System.out.println(entry2.getKey()+" : "+entry2.getValue());
            }
        }
    }

    public void initvalueFromMarkovCover() {



/*
        Domain.DomainValue maxValue = null;

        AbstractDouble maxProb = this.probabilityCompute.getDoubleFactory().getNew(0.0);

        for(Domain.DomainValue domainValue : this.domain.getValues()){

            this.setDomainValue(domainValue);

            AbstractDouble prob = this.getProbabilityForCurrentValue();

            for( Variable child : this.children){

                prob = prob.multiply(child.getProbabilityForCurrentValue());
            }

            if(prob.compareTo(maxProb) >= 0){

                maxValue = domainValue;
            }

        }

        this.setDomainValue(maxValue);

        */
    }

    public void initRdmValue() {

        this.setDomainValue(this.probabilityCompute.getRandomValue(this));
    }

    public IDomain getDomain() {
        return domain;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Variable variable = (Variable) o;
        return Objects.equals(label, variable.label);
    }

    @Override
    public int hashCode() {

        return Objects.hash(label);
    }

    public boolean isRoot() {

        return this.dependencies.isEmpty();
    }

    public List<Variable> getChildren() {
        return children;
    }

    public boolean isAdded() {
        return added;
    }

    public void setAdded(boolean added) {
        this.added = added;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @Override
    public String toString() {

        return this.label + " " + this.value;//+"=======\n"+this.probabilityCompute.toString();
    }

    public Variable simpleCopy() {

        return new Variable(this);
    }

    public String getValueKey() {

        return this.getValue().toString();
    }

    public int getDomainSize() {

        return this.domain.getValues().size();
    }

    public Iterable<Domain.DomainValue> getDomainValues() {

        return this.domain.getValues();
    }

    public void addFactor(Factor factor) {

        this.factors.add(factor);
    }

    public void addActiveFactor(Factor factor) {

        this.activeFactors.add(factor);
    }

    public List<Factor> getActiveFactors() {

        return activeFactors;
    }

    public void clearFactors() {

        this.factors.clear();

        this.activeFactors.clear();

        //inutile de reinitialiser le compteur de variables initialisés pour ces facteurs
        //qui passeront au ramasse miettes
    }

    public boolean isObs() {
        return isObs;
    }

    public void setObs(boolean obs) {
        isObs = obs;
    }

}
