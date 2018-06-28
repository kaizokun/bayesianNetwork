package network.dynamic;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import math.Matrix;
import math.Transpose;
import network.BayesianNetwork;
import network.Variable;

import java.util.*;

public class MMC extends DynamicBayesianNetwork {

    protected Variable megaVariableStates0, megaVariableStates1, megaVariableObs1;

    protected Map<String, Matrix> matrixObs;

    protected Matrix matrixState0, matrixStates, matrixStatesT;

    public MMC(Variable[] states0, Variable[] states1, Variable[] obs1, AbstractDoubleFactory doubleFactory) {

        super(doubleFactory);

        this.time++;

        Arrays.sort(states0, Variable.varLabelComparator);

        Arrays.sort(states1, Variable.varLabelComparator);

        Arrays.sort(obs1, Variable.varLabelComparator);

        this.megaVariableStates0 = this.mergeStateVariables(Arrays.asList(states0), 0);

        this.megaVariableStates1 = this.mergeStateVariables(Arrays.asList(states1), 1);

        this.megaVariableObs1 = this.mergeObservationVariables(Arrays.asList(obs1), Arrays.asList(states1), 1);

        this.megaVariableStates1.addDependency(this.megaVariableStates0);

        this.megaVariableObs1.addDependency(this.megaVariableStates1);

        this.getTimeVariables(0).put(megaVariableStates0, megaVariableStates0);

        this.getTimeVariables(1).put(megaVariableStates1, megaVariableStates1);

        this.getTimeVariables(1).put(megaVariableObs1, megaVariableObs1);

        this.roots.add(this.megaVariableStates0);
    }

    @Override
    public void extend() {

        this.time++;

        Variable lastState = this.getTimeVariables(this.time - 1).get(this.megaVariableStates1);

        Variable lastObs = this.getTimeVariables(this.time - 1).get(this.megaVariableObs1);

        Variable newState = new Variable(lastState.getCompoVars(), this.time);

        newState.addDependency(lastState);

        Variable newObs = new Variable(lastObs.getCompoVars(), this.time);

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

            matrixMap.put(obsDomainValues.toString(), new Matrix(obsMatrix, obs, states, doubleFactory, true));
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

    public Matrix forward(int t, Map<Integer, Variable> megaVariableObs) {

        return forward(t, megaVariableObs, 0);
    }

    private Matrix forward(int t, Map<Integer, Variable> megaVariableObs, int depth) {

        /*
         * on pourrait faire des megavariables états observations sur des sous ensemble de variables du reseau
         * et pas forcement sur la totalité et au besoin
         *
         * */

        //System.out.println( Util.getIdent(depth)+" "+t);

        if (t == 0) {

            Matrix forward = new Transpose(this.matrixState0);

            forward.normalize();

            //System.out.println("FORWARD "+t+"\n "+forward);

            return forward;
        }

        Variable megaObs = megaVariableObs.get(t);

        Matrix obs = this.getMatrixObs(megaObs);
        //dans la matrice de base les lignes correspondent aux valeurs parents
        //et les colones aux valeurs enfants, dans la transposée c'est l'inverse.
        //la multiplication matricielle se fait ligne par ligne pour la transposée de la premiere matrice
        //soit valeur par valeur de la megavariable états
        //puis pour chaque ligne la somme se fait colonne par colonne
        //soit pour chaque valeur prise par la megavariable parents située au temps précédent
        //la matrice resultante contient une ligne par  valeur de la megavariable (enfant) en temps t
        //la matrice observation contient une valeur par ligne pour chaque valeur de la la megavariable en temps t
        //ces valeurs sont sur la diagonale le reste à zero pour faciliter le calcul
        //en multipliant la matrice somme par la matrice observation on obtient la distribution forward
        //sur les valeurs de la megavariable en temps t ligne par ligne
        Matrix sum = this.matrixStatesT.multiply(forward(t - 1, megaVariableObs, depth + 1));

        Matrix forward = obs.multiply(sum);

        return forward.normalize();
    }

    public Matrix backward(int t, Map<Integer, Variable> megaVariableObs) {

        return backward(t, megaVariableObs, 0);
    }

    private Matrix backward(int t, Map<Integer, Variable> megaVariableObs, int depth) {

        if(t == this.time){

            int rows = 1;
            //la matrice limite contient uniquement des valerus à 1
            //et autant de lignes qu'il y a de combinaisons de valeurs pour les variables états
            for( Variable state : this.megaVariableStates1.getCompoVars()){

                rows *= state.getDomainSize();
            }

            //une ligne par valeur
            AbstractDouble[][] limitMatrix = new AbstractDouble[rows][1];

            for(int row = 0 ; row < rows ; row ++ ){

                limitMatrix[row][0] = doubleFactory.getNew(1.0);
            }

            Matrix backward = new Matrix(limitMatrix, doubleFactory);

            return backward;
        }

        Variable megaObs = megaVariableObs.get(t);

        Matrix obs = this.getMatrixObs(megaObs);

        Matrix transition = this.matrixStates;

        Matrix backWard = this.backward(t + 1, megaVariableObs, depth + 1);

        return obs.multiply(transition).multiply(backWard).normalize();
    }


    /*-------------------- GETTER SETTER --------------------*/

    public Variable getMegaVariable(int time, Variable... variables) {

        // trie les variables constituant la megavariable par labels
        Arrays.sort(variables, Variable.varLabelComparator);
        // crée une variable clé conenant le label composée
        Variable megaVariable = new Variable(variables);
        // recupere la variable enregistrée dans le reseau au temps t à l'aide de ce label
        return this.getVariable(time, megaVariable);
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
