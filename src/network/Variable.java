package network;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class Variable {

    public static Comparator<Variable> varLabelComparator;

    static{

        varLabelComparator = new Comparator<Variable>() {
            @Override
            public int compare(Variable v1, Variable v2) {
                return v1.getLabel().compareTo(v2.getLabel());
            }
        };
    }

    protected IDomain domain;

    protected String label;

    protected Domain.DomainValue value, originValue;

    protected List<Variable> dependencies;

    protected List<Variable> children;

    protected ProbabilityCompute probabilityCompute;

    protected MarkovCoverDistributionCompute markovCoverDistributionCompute;

    protected Set<Variable> markovKover;

    protected Hashtable<String, Integer> dependenciesIndex;

    protected List<Variable> observations;

    protected List<Variable> compoVars;

    protected int time;

    protected AbstractDouble[][] matrix;

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

    public Variable(String label, IDomain domain){

        this.domain = domain;

        this.label = label;
    }

    public Variable(String label, IDomain domain, Collection<Variable> compoVars){

        this.domain = domain;

        this.label = label;

        this.compoVars = new LinkedList<>(compoVars);
    }

    public Variable(String label){

        this.label = label;
    }

    public Variable(String label, IDomain domain, ProbabilityCompute probabilityCompute ) {

        this(label, domain, probabilityCompute, new ArrayList<>(),0);
    }

    public Variable(String label, IDomain domain, ProbabilityCompute probabilityCompute, List<Variable> dependencies ) {

        this(label, domain, probabilityCompute, dependencies,0);
    }

    public Variable(String label, IDomain domain, ProbabilityCompute probabilityCompute, List<Variable> dependencies, int time ) {

        this.label = label;

        this.domain = domain;

        this.dependencies = dependencies;

        this.probabilityCompute = probabilityCompute;

        this.children = new ArrayList<>();

        this.dependenciesIndex = new Hashtable<>();

        this.observations = new LinkedList<>();

        this.time = time;

        int i = 0;

        for (Variable parent : dependencies) {
            //ajout de la variable courante comme enfant du parent
            parent.addChild(this);
            //enregistre l'id du parent dans le tableau
            //ici plusieurs meme variable peuvent avoir des temps differents
            //donc on recupere une clé composée du label suivit du temps de la variable parent
            this.dependenciesIndex.put(getVarTimeId(parent, parent.time), i ++);
        }
    }

    public Variable(String label, int time, Domain.DomainValue value) {

        this.label = label;

        this.time = time;

        this.value = value;
    }

    public  String getVarTimeId(){

        return this.getVarTimeId(this, this.time);
    }

    private String getVarTimeId(Variable variable, int time){

        return variable.getLabel()+"_"+time;
    }

    public Variable getParent(int time){

        String varTimeId = getVarTimeId(this, time);

        int indexId = this.dependenciesIndex.get(varTimeId);

        return this.dependencies.get(indexId);
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

    /**
     * return the probability of the variable from its current value et its dependencies values
     * */
    public AbstractDouble getProbabilityForCurrentValue() {

        return probabilityCompute.getProbability(this);
    }

    public void loadMarkovCover(){

        this.markovKover = new LinkedHashSet<>();

        this.markovKover.addAll(this.dependencies);

        for(Variable child : this.children){

            this.markovKover.addAll(child.dependencies);
        }

        this.markovKover.addAll(this.children);

        this.markovKover.remove(this);
    }

    public void loadMarkovCover(List<Variable> obs){

        this.loadMarkovCover();

        this.markovKover.removeAll(obs);
    }

    public void initCumulativeMarkovFrequencies(List<Variable> obs) {

        this.markovCoverDistributionCompute.initCumulativeMarkovFrequencies(obs, this);
    }

    public void initRandomValueFromMarkovCover() {

       this.setDomainValue(this.markovCoverDistributionCompute.getRandomValueFromMarkovCover(this));
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

    @Override
    public String toString() {

        return this.label+"_"+this.time+" = "+this.value;
    }

    public String getValueKey() {

        return this.getValue().toString();
    }

    public Variable copyLabelTimeValue(){

        return new Variable(this.label, this.time, this.value);
    }

    public int getDomainSize() {

        return this.domain.getValues().size();
    }

    public void setDomain(IDomain domain) {
        this.domain = domain;
    }

    public Iterable<Domain.DomainValue> getDomainValues() {

        return this.domain.getValues();
    }

    public ProbabilityCompute getProbabilityCompute() {
        return probabilityCompute;
    }

    public void setProbabilityCompute(ProbabilityCompute probabilityCompute) {
        this.probabilityCompute = probabilityCompute;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
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

    public AbstractDoubleFactory doubleFactory(){

       return this.probabilityCompute.getDoubleFactory();
    }

    public void setMarkovCoverDistributionCompute(MarkovCoverDistributionCompute markovCoverDistributionCompute) {
        this.markovCoverDistributionCompute = markovCoverDistributionCompute;
    }

    public boolean hasDependencies() {

        return !this.dependencies.isEmpty();
    }

    public void saveObservation() {

        for(Variable dep : dependencies){

            dep.observations.add(this);
        }
    }

    public List<Variable> getObservations() {

        return observations;
    }

    public List<Variable> getCompoVars() {
        return compoVars;
    }

    public void setCompoVars(List<Variable> compoVars) {
        this.compoVars = compoVars;
    }

    public AbstractDouble[][] getMatrix() {
        return matrix;
    }

    public void setMatrix(AbstractDouble[][] matrix) {

        this.matrix = matrix;
    }

    public String getMatrixView(){

        if(matrix == null){

            return "EMPTY MATRX";
        }

        List<List<Domain.DomainValue>> domainValuesLists = BayesianNetwork.domainValuesCombinations(this.compoVars);

        StringBuilder builder = new StringBuilder("\n");

        builder.append(label+'\n');

        if(matrix.length > 1) {

            builder.append(String.format("%6s", ""));
        }

        for(List<Domain.DomainValue> domainValues : domainValuesLists){

            builder.append(String.format("%-7s", domainValues));
        }

        builder.append('\n');

        int r = 0 ;

        for(AbstractDouble[] row : this.matrix){

            if(matrix.length > 1) {

                builder.append(String.format("%5s", domainValuesLists.get(r)));
            }

            for(AbstractDouble col : row){

                builder.append(String.format("[%.3f]", col.getDoubleValue()));
            }

            builder.append('\n');

            r ++;
        }

        return builder.toString();
    }
}
