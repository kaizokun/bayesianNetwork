package network.dynamic;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import inference.dynamic.mmc.ForwardMMC;
import inference.dynamic.mmc.SmoothingMMC;
import inference.dynamic.mmc.SmoothingMMC.SmoothingMatrices;
import math.Matrix;
import math.Transpose;
import network.BayesianNetwork;
import network.Variable;

import java.util.*;

public class MMC extends DynamicBayesianNetwork {

    protected Variable megaVariableStatesRoot, megaVariableStates, megaVariableObs;

    protected Variable[] obsVariables;

    protected Map<String, Matrix> matrixObs;

    protected Matrix matrixState0, matrixStates, matrixStatesT;

    protected Map.Entry<Integer,Matrix> LastForward;
    //end decalage avant time pour la fin du lissage, start decalage avant time pour le debut du lissage
    protected int smootStart = 1, smootEnd = 1;

    protected Map<Integer, SmoothingMatrices> smoothings = new Hashtable<>();

   // protected Map<Integer, Integer> smootRange = new Hashtable<>();

    protected ForwardMMC forwardMMC;

    protected SmoothingMMC smoothingMMC;

    public MMC(Variable[] statesRoot, Variable[] states, Variable[] obs, AbstractDoubleFactory doubleFactory) {

        super(doubleFactory);
        //trie les variables par labels
        Arrays.sort(statesRoot, Variable.varLabelComparator);

        Arrays.sort(states, Variable.varLabelComparator);

        Arrays.sort(obs, Variable.varLabelComparator);
        //sauvegarde les variables observations
        this.obsVariables = obs;

        this.megaVariableStatesRoot = this.mergeStateVariables(Arrays.asList(statesRoot), 0);

        this.megaVariableStates = this.mergeStateVariables(Arrays.asList(states), 1);

        this.megaVariableObs = this.mergeObservationVariables(Arrays.asList(obs), Arrays.asList(states), 1);

        this.getTimeVariables(0).put(megaVariableStatesRoot, megaVariableStatesRoot);

        this.roots.add(this.megaVariableStatesRoot);

       // this.smootRange.put(0, 0);
    }

    public void extend(Variable[][] variablesTab) {

        this.extend( variablesTab, false);
    }

    public void extend(Variable[][] variablesTab, boolean log) {

        //pour chaque nouvelles observations
        for (Variable variables[] : variablesTab) {

            System.out.println("\n-----------------");
            System.out.println("Extend "+time+" -> "+(time + 1));
            System.out.println("-----------------\n");

            this.extend(variables);

            if(log){


            }
        }
    }

    public void extend(Variable[] variables) {

        //on etend le reseau
        this.extend();
        //on recupere la nouvelle megavariable observation
        Variable megaVariableObservation = this.getMegaVariableObs(this.time);
        //on l'initialise à l'aide d'une liste de variables qui la compose et initialisées
        megaVariableObservation.setDomainValuesFromVariables(variables);
        //appliquer le forward sur le dernier etats
        this.forwardMMC.forward();
        //ainsi que le smoothing sur le range souhaité
        this.smoothingMMC.smoothing();
    }

    @Override
    public void extend() {

        this.time++;

        Variable newState = new Variable(this.megaVariableStates.getCompoVars(), this.time);

        Variable newObs = new Variable(this.megaVariableObs.getCompoVars(), this.time);
        //si time - 1 vaut zero on recuperera la megavariable root qui a le même label que celles qui la succede...
        newState.addDependency(this.getTimeVariables(this.time - 1).get(this.megaVariableStates));

        newObs.addDependency(newState);

        this.getTimeVariables(this.time).put(newState, newState);

        this.getTimeVariables(this.time).put(newObs, newObs);
    }


    private void loadVarDistrib(AbstractDouble[] row, List<Variable> vars, List<List<Domain.DomainValue>> domainValuesList, AbstractDoubleFactory doubleFactory) {

        int col = 0;
        //pour chaque combinaisons de valeurs pouvant être prises par les variables
        for (List<Domain.DomainValue> domainValues : domainValuesList) {

            Iterator<Variable> tVarsIterator = vars.iterator();
            //assigne une combinaison de valeurs aux variables
            for (Domain.DomainValue domainValue : domainValues) {

                tVarsIterator.next().setDomainValue(domainValue);
            }

            AbstractDouble prob = doubleFactory.getNew(1.0);

            tVarsIterator = vars.iterator();
            //multiplie les probabilités
            while (tVarsIterator.hasNext()) {

                prob = prob.multiply(tVarsIterator.next().getProbabilityForCurrentValue());
            }

            row[col] = prob;

            col++;
        }
    }

    private Variable mergeObservationVariables(List<Variable> obs, List<Variable> states, int time) {

        //combinaison de valeurs pour les parents des observations
        List<List<Domain.DomainValue>> statesDomainValuesList = BayesianNetwork.domainValuesCombinations(states);

        //récuperer les combinaisons de valeurs on aura une matrice par valeur
        //que peuvent prendre le sobservations à un temps t
        //stockées dans la megaVariable et récuperables en fonction de la combinaison de valeurs
        //qui formera la clé pour chaque matrice
        List<List<Domain.DomainValue>> obsDomainValuesList = BayesianNetwork.domainValuesCombinations(obs);

        //liste des matrices
        Map<String, Matrix> matrixMap = new Hashtable<>();
        //pour chaque combinaison de valeur prises par toutes les observations
        for (List<Domain.DomainValue> obsDomainValues : obsDomainValuesList) {
            //initialisation des observations
            Iterator<Variable> tObsIterator = obs.iterator();

            for (Domain.DomainValue obsDomainValue : obsDomainValues) {

                tObsIterator.next().setDomainValue(obsDomainValue);
            }

            AbstractDouble[][] obsMatrix = new AbstractDouble[statesDomainValuesList.size()][statesDomainValuesList.size()];

            for (int r = 0; r < statesDomainValuesList.size(); r++) {
                for (int c = 0; c < statesDomainValuesList.size(); c++) {
                    obsMatrix[r][c] = doubleFactory.getNew(0.0);
                }
            }

            int col = 0;

            //calculer la probabilité d'un état des observations pour une combinaison de valeurs parents
            for (List<Domain.DomainValue> statesDomainValues : statesDomainValuesList) {

                //initialise les valeurs parents
                Iterator<Variable> tStatesIterator = states.iterator();

                for (Domain.DomainValue stateDomainValue : statesDomainValues) {

                    tStatesIterator.next().setDomainValue(stateDomainValue);
                }

                AbstractDouble prob = doubleFactory.getNew(1.0);

                tObsIterator = obs.iterator();

                while (tObsIterator.hasNext()) {

                    prob = prob.multiply(tObsIterator.next().getProbabilityForCurrentValue());
                }

                obsMatrix[col][col] = prob;

                col++;
            }

            matrixMap.put(obsDomainValues.toString(), new Transpose(obsMatrix, obs, states, doubleFactory, true));
        }

        this.matrixObs = matrixMap;

        return new Variable(obs, time);
    }

    private Variable mergeStateVariables(List<Variable> states, int time) {

        //List<Variable> states =  copyTimeVarsAndSort(t, statesToMerge);

        List<Variable> parentStates = new ArrayList<>();

        List<List<Domain.DomainValue>> domainValuesList = BayesianNetwork.domainValuesCombinations(states);

        AbstractDouble[][] matrix;
        //si variables de temps 0
        if (time == 0) {

            matrix = new AbstractDouble[1][domainValuesList.size()];

            this.loadVarDistrib(matrix[0], states, domainValuesList, doubleFactory);

            this.matrixState0 = new Matrix(matrix, states, null, doubleFactory, false);

        } else {

            matrix = new AbstractDouble[domainValuesList.size()][domainValuesList.size()];
            //Set des variables parents
            Set<Variable> parents = new LinkedHashSet<>();

            for (Variable state : states) {

                parents.addAll(state.getDependencies());
            }

            parentStates.addAll(parents);

            Collections.sort(parentStates, Variable.varLabelComparator);
            //enregistre les états parents qui restent identiques pour un MMC
            //this.parentStates = parentStates;

            int row = 0;
            //pour chaque combinaisons de valeurs pouvant être prises par les variables parents
            //ici on boucle sur les combinaisons obtenus à partir de la liste des états
            //pour charger les parents, c'est en fait la même liste trié de la même manière...
            for (List<Domain.DomainValue> domainValuesParents : domainValuesList) {

                Iterator<Variable> tVarsParentsIterator = parentStates.iterator();
                //assigne une combinaison de valeurs aux variables
                for (Domain.DomainValue domainValue : domainValuesParents) {

                    tVarsParentsIterator.next().setDomainValue(domainValue);
                }

                this.loadVarDistrib(matrix[row], states, domainValuesList, doubleFactory);

                row++;
            }

            this.matrixStates = new Matrix(matrix, states, parentStates, doubleFactory, false);

            this.matrixStatesT = new Transpose(this.matrixStates);
        }

        return new Variable(states, time);
    }

    /*-------------------- GETTER SETTER --------------------*/
/*
    public Variable getMegaVariable(int time, Variable... variables) {

        // trie les variables constituant la megavariable par labels
        Arrays.sort(variables, Variable.varLabelComparator);
        // crée une variable servant de clé contenant le label composée
        Variable megaVariable = new Variable(variables);
        // recupere la variable enregistrée dans le reseau au temps t à l'aide de ce label
        return this.getVariable(time, megaVariable);
    }
*/
    public Variable getMegaVariableObs(int time) {

        return this.getVariable(time, this.megaVariableObs);
    }

    public Variable getMegaVariableState(int time) {

        return this.getVariable(time, this.megaVariableStates);
    }

    public Matrix getMatrixState0() {

        return matrixState0;
    }

    public Matrix getMatrixStates() {

        return matrixStates;
    }

    public Matrix getMatrixObs(Variable megaVariable) {

        return matrixObs.get(megaVariable.getMegaVarValuesKey());
    }

    public Matrix getMatrixStatesT() {

        return this.matrixStatesT;
    }

    public Variable getMegaVariableStatesRoot() {
        return megaVariableStatesRoot;
    }

    public Variable getMegaVariableStates() {
        return megaVariableStates;
    }

    public Variable getMegaVariableObs() {
        return megaVariableObs;
    }

    public Map<String, Matrix> getMatrixObs() {
        return matrixObs;
    }

    public Map.Entry<Integer, Matrix> getLastForward() {
        return LastForward;
    }

    public void setLastForward(Map.Entry<Integer, Matrix> lastForward) {
        LastForward = lastForward;
    }

    public int getSmootStart() {
        return smootStart;
    }

    public void setSmootStart(int smootStart) {
        this.smootStart = smootStart;
    }

    public int getSmootEnd() {
        return smootEnd;
    }

    public void setSmootEnd(int smootEnd) {
        this.smootEnd = smootEnd;
    }

    public Map<Integer, SmoothingMatrices> getSmoothings() {
        return smoothings;
    }

    public void setSmoothings(Map<Integer, SmoothingMatrices> smoothings) {
        this.smoothings = smoothings;
    }
/*
    public Map<Integer, Integer> getSmootRange() {
        return smootRange;
    }

    public void setSmootRange(Map<Integer, Integer> smootRange) {
        this.smootRange = smootRange;
    }
*/
    public ForwardMMC getForwardMMC() {
        return forwardMMC;
    }

    public void setForwardMMC(ForwardMMC forwardMMC) {
        this.forwardMMC = forwardMMC;
    }

    public SmoothingMMC getSmoothingMMC() {
        return smoothingMMC;
    }

    public void setSmoothingMMC(SmoothingMMC smoothingMMC) {
        this.smoothingMMC = smoothingMMC;
    }

    /*---------------------- VIEW ---------------*/

    @Override
    public String toString() {

        StringBuilder stringBuilder = new StringBuilder(super.toString());

        stringBuilder.append("--------------------------------------------------------------------\n");
        stringBuilder.append("--------------------------- MATRIX ---------------------------------\n");
        stringBuilder.append("--------------------------------------------------------------------\n\n");

        for (Matrix matrixObs : matrixObs.values()) {

            stringBuilder.append(matrixObs);

            stringBuilder.append('\n');
        }

        stringBuilder.append(matrixState0);

        stringBuilder.append('\n');

        stringBuilder.append(matrixStates);

        return stringBuilder.toString();
    }



}
