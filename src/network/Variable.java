package network;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class Variable {

    protected int depth;

    protected IDomain domain;

    protected String label;

    protected Domain.DomainValue value, originValue;

    protected List<Variable> dependencies;

    protected List<Variable> children;

    protected ProbabilityCompute probabilityCompute;
    //utilisé dans la recuperation des noeuds dans l'odre topologique
    //pour savoir si ils ont été ajoutés, pas vraiment une propriété mais plus pratique que de créer
    //des tables de hachages à chaque.
    protected boolean added, important, isObs;

    protected int tempIndex;

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

        for(Variable parent : dependencies){

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

    public void checkActiveFactors(){

        for(Factor factor : factors){

            factor.ctpVarInit(this);
        }
    }

    public boolean isInit() {
        return this.value != null;
    }

    public void clear(){
        this.value = null;
    }

    public List<Variable> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<Variable> dependencies) {
        this.dependencies = dependencies;
    }

    public AbstractDouble getProbabilityForCurrentValue(){

        return probabilityCompute.getProbability(this);
    }

    public void initRdmValue(){

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

        return this.label+" "+this.value;//+"=======\n"+this.probabilityCompute.toString();
    }

    public Variable simpleCopy(){

        return new Variable(this);
    }

    public Variable sampleCopy(){

        Variable variable = new Variable();

        variable.setDomainValue(this.value);

        variable.setLabel(this.label);

        variable.setTempIndex(this.tempIndex);

        return variable;
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

    public void addFactor(Factor factor){

        this.factors.add(factor);
    }

    public void addActiveFactor(Factor factor){

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

    public int getTempIndex() {
        return tempIndex;
    }

    public void setTempIndex(int tempIndex) {
        this.tempIndex = tempIndex;
    }

    public boolean isObs() {
        return isObs;
    }

    public void setObs(boolean obs) {
        isObs = obs;
    }

}
