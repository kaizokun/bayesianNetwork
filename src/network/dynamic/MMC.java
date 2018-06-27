package network.dynamic;

import domain.Domain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import math.Matrix;
import network.BayesianNetwork;
import network.Variable;

import java.util.*;

public class MMC extends DynamicBayesianNetwork {

    protected Variable megaVariableStates0, megaVariableStates1, megaVariableObs1;

    protected Map<String, Matrix> matrixObs;

    protected Matrix matrixState0, matrixStates;

    protected List<Variable> states, obs, parentStates, parentObs;

    public MMC(List<Variable> states0, List<Variable> states1, List<Variable> obs1, AbstractDoubleFactory doubleFactory) {

        super(doubleFactory);

        this.time ++;

        Collections.sort(states0, Variable.varLabelComparator);

        Collections.sort(states1, Variable.varLabelComparator);

        Collections.sort(obs1, Variable.varLabelComparator);

        this.states = states0;

        this.obs = obs1;

        this.parentObs = states1;

        this.megaVariableStates0 = this.mergeStateVariables(states0, 0);

        this.megaVariableStates1 = this.mergeStateVariables(states1, 1);

        this.megaVariableObs1 = this.mergeObservationVariables(obs1, states1, 1);

        this.megaVariableStates1.addDependency(this.megaVariableStates0);

        this.megaVariableObs1.addDependency(this.megaVariableStates1);

        this.getTimeVariables(0).put(megaVariableStates0, megaVariableStates0);

        this.getTimeVariables(1).put(megaVariableStates1, megaVariableStates1);

        this.getTimeVariables(1).put(megaVariableObs1, megaVariableObs1);

        this.roots.add(this.megaVariableStates0);
    }

    @Override
    public void extend() {

        this.time ++;

        Variable lastState = this.getTimeVariables(this.time - 1).get(this.megaVariableStates1);

        Variable lastObs = this.getTimeVariables(this.time - 1).get(this.megaVariableObs1);

        Variable newState = new Variable(lastState.getCompoVars(), this.time);

        newState.addDependency(lastState);

        Variable newObs = new Variable(lastObs.getCompoVars(), this.time);

        newObs.addDependency(newState);

        this.getTimeVariables(this.time).put(newState, newState);

        this.getTimeVariables(this.time).put(newObs, newObs);
    }

    private void loadVarDistrib(AbstractDouble[] row, List<Variable> vars, List<List<Domain.DomainValue>> domainValuesList, AbstractDoubleFactory doubleFactory){

        int col = 0;
        //pour chaque combinaisons de valeurs pouvant être prises par les variables
        for(List<Domain.DomainValue> domainValues : domainValuesList){

            Iterator<Variable> tVarsIterator = vars.iterator();
            //assigne une combinaison de valeurs aux variables
            for(Domain.DomainValue domainValue : domainValues){

                tVarsIterator.next().setDomainValue(domainValue);
            }

            AbstractDouble prob = doubleFactory.getNew(1.0);

            tVarsIterator = vars.iterator();
            //multiplie les probabilités
            while(tVarsIterator.hasNext()){

                prob =  prob.multiply( tVarsIterator.next().getProbabilityForCurrentValue());
            }

            row[col] = prob;

            col ++;
        }
    }

    private Variable  mergeObservationVariables(List<Variable> obs, List<Variable> states, int time){

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
        for(List<Domain.DomainValue> obsDomainValues : obsDomainValuesList){
            //initialisation des observations
            Iterator<Variable> tObsIterator = obs.iterator();

            for(Domain.DomainValue obsDomainValue : obsDomainValues){

                tObsIterator.next().setDomainValue(obsDomainValue);
            }

            AbstractDouble[][] obsMatrix = new AbstractDouble[statesDomainValuesList.size()][statesDomainValuesList.size()];

            for(int r = 0 ; r < statesDomainValuesList.size() ; r ++) {
                for (int c = 0; c < statesDomainValuesList.size(); c++) {
                    obsMatrix[r][c] = doubleFactory.getNew(0.0);
                }
            }

            int col = 0;

            //calculer la probabilité d'un état des observations pour une combinaison de valeurs parents
            for(List<Domain.DomainValue> statesDomainValues : statesDomainValuesList){

                //initialise les valeurs parents
                Iterator<Variable> tStatesIterator = states.iterator();

                for(Domain.DomainValue stateDomainValue : statesDomainValues){

                    tStatesIterator.next().setDomainValue(stateDomainValue);
                }

                AbstractDouble prob = doubleFactory.getNew(1.0);

                tObsIterator = obs.iterator();

                while(tObsIterator.hasNext()){

                    prob = prob.multiply( tObsIterator.next().getProbabilityForCurrentValue() );
                }

                obsMatrix[col][col] = prob;

                col ++;
            }

            matrixMap.put(obsDomainValues.toString(), new Matrix(obsMatrix, doubleFactory));
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
        if(time == 0){

            matrix = new AbstractDouble[1][domainValuesList.size()];

            this.loadVarDistrib(matrix[0], states, domainValuesList, doubleFactory);

            this.matrixState0 = new Matrix(matrix, doubleFactory);

        }else{

            matrix = new AbstractDouble[domainValuesList.size()][domainValuesList.size()];
            //Set des variables parents
            Set<Variable> parents = new LinkedHashSet<>();

            for(Variable state : states){

                parents.addAll(state.getDependencies());
            }

            parentStates.addAll(parents);

            Collections.sort(parentStates, Variable.varLabelComparator);
            //enregistre les états parents qui restent identiques pour un MMC
            this.parentStates = parentStates;

            int row = 0;
            //pour chaque combinaisons de valeurs pouvant être prises par les variables parents
            for(List<Domain.DomainValue> domainValuesParents : domainValuesList){

                Iterator<Variable> tVarsParentsIterator = parentStates.iterator();
                //assigne une combinaison de valeurs aux variables
                for(Domain.DomainValue domainValue : domainValuesParents){

                    tVarsParentsIterator.next().setDomainValue(domainValue);
                }

                this.loadVarDistrib(matrix[row], states, domainValuesList, doubleFactory);

                row ++;
            }

            this.matrixStates = new Matrix(matrix, doubleFactory);
        }

        return new Variable(states, time);
    }

    public void showMegaVarsMatrix(){

        List<List<Domain.DomainValue>> compVarParentsValues = BayesianNetwork.domainValuesCombinations(parentStates);

        System.out.println(getMatrixView(megaVariableStates0, matrixState0.getMatrix(), "", compVarParentsValues, compVarParentsValues));

        System.out.println(getMatrixView(megaVariableStates1, matrixStates.getMatrix(), "", compVarParentsValues, compVarParentsValues));

        System.out.println(getObsMatrixView());
    }

    public String getObsMatrixView(){

        StringBuilder builder = new StringBuilder();

        List<List<Domain.DomainValue>> compVarParentsValues = BayesianNetwork.domainValuesCombinations(parentObs);

        List<List<Domain.DomainValue>> compVarValues = BayesianNetwork.domainValuesCombinations(obs);

        for(Map.Entry<String, Matrix> entry : this.matrixObs.entrySet()){

            builder.append(getMatrixView(megaVariableObs1, entry.getValue().getMatrix(), entry.getKey(), compVarValues, compVarParentsValues ));
        }

        return builder.toString();
    }

    public String getMatrixView(Variable megaVariable, AbstractDouble[][] matrix, String key,
                                List<List<Domain.DomainValue>> compVarValues,
                                List<List<Domain.DomainValue>> compVarParentsValues ){

        StringBuilder builder = new StringBuilder("\n");

        builder.append(megaVariable.getLabel()+"-"+megaVariable.getTime()+" : "+key+'\n');

        if(key.isEmpty()) {

            if(matrix.length > 1) {

                builder.append(String.format("%6s", ""));
            }

            for (List<Domain.DomainValue> domainValues : compVarValues) {

                builder.append(String.format("%-7s", domainValues));
            }

            builder.append('\n');
        }

        int r = 0 ;

        for(AbstractDouble[] row : matrix){

            if(matrix.length > 1) {

                builder.append(String.format("%5s", compVarParentsValues.get(r)));
            }

            for(AbstractDouble col : row){

                builder.append(String.format("[%.3f]", col.getDoubleValue()));
            }

            builder.append('\n');

            r ++;
        }

        return builder.toString();
    }

    public Matrix getMatrixState0() {

        return matrixState0;
    }

    public Matrix getMatrixStates() {

        return matrixStates;
    }
}
