package network;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class ProbabilityComputeFromTCP implements ProbabilityCompute{

    protected Hashtable<String, Hashtable<String, AbstractDouble>> TCP;

    public ProbabilityComputeFromTCP( IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory){

        this(new ArrayList<>(), varDom, entries, doubleFactory);
    }

    public ProbabilityComputeFromTCP(List<Variable> dependencies, IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory){

        this.TCP = new Hashtable<>();

        this.initCTP(dependencies, varDom, entries, new LinkedList<>(), 0, doubleFactory);
    }

    /**
     * Génere une clé à partir d'une combinaison de valeurs pour les variables parents
     * **/
    private String getDependenciesValuesKey(List<String> keyParts){

        StringBuilder key = new StringBuilder();

        for(String keyPart : keyParts){

            key.append(keyPart);

            key.append("-");
        }

        if(key.length() != 0) {

            key.deleteCharAt(key.length() - 1);
        }

        return key.toString();
    }

    private void initCTP(List<Variable> dependencies, IDomain varDom, Double[][] entries, LinkedList<String> keyParts, int iDep, AbstractDoubleFactory doubleFactory){

        //Double[][] entries : valeur de la TCP : la premier dimension correspond à une combinaison de valeur pour les parents
        //la deuxiemme correspond aux probabilités pour les valeurs du domaine de la variable


        //quand le dernier parent a été atteind on à une combinaison de valeur pour chaque parent
        //formant la clé d'une entrée ou ligne de la TCP
        if(iDep == dependencies.size()){

            //genere la clé sous forme de chaine de charactère à partir des valeurs
            String key = this.getDependenciesValuesKey(keyParts);

            //récupere l'indice de la ligne de la TCP

            int iRow = this.TCP.keySet().size();

            //table indexé par valeur prises par la variable et contenant la probabilité en fonction des valeurs
            //prises par les parents

            Hashtable<String, AbstractDouble> row = new Hashtable<>();

            //pour chaque valeur du domaine de la variable
            //on enregistre la probabilité
            //la liste des entrées doit correspondre avec l'ordre des valeurs du domaine de la variable
            for(int j = 0 ; j < varDom.getSize() ; j ++ ){

                //si une chaine est trop longue pour une valeur
                //il faudrait mieux encapsuler les valeur de domaines dans une classe
                //qui contiendrait la aleur sous forme d'objet et un alias plus court
                //pour l'indexation
                String domainValue = varDom.getObjectValue(j).toString();

                AbstractDouble prob = doubleFactory.getNew(entries[iRow][j]);

                row.put(domainValue, prob);
            }

            //ajout de la ligne dans la TCP
            this.TCP.put(key, row);

            return;
        }

        IDomain dependencieDomain = dependencies.get(iDep).getDomain();

        for( Domain.DomainValue o : dependencieDomain.getValues()){

            keyParts.addLast(o.getValue().toString());

            this.initCTP(dependencies, varDom, entries, keyParts, iDep + 1, doubleFactory);

            keyParts.removeLast();
        }

    }

    @Override
    public AbstractDouble getProbability(List<Variable> dependencies, Variable var) {

        //création de la clé correspondant à la combinaison de valeur des parents
        List<String> keyParts = new LinkedList<>();

        for(Variable dep : dependencies){

            keyParts.add(dep.getValue().toString());
        }

        String depKey = getDependenciesValuesKey(keyParts);

        String varKey = var.getValueKey();

        //System.out.println("depkey "+depKey);
        //System.out.println("varkey "+varKey);

        return this.TCP.get(depKey).get(varKey);
    }

    @Override
    public String toString() {

        StringBuilder  builder = new StringBuilder();

        for(String key : this.TCP.keySet()){

            if(!key.isEmpty()) {

                builder.append(key);

                builder.append(" : ");
            }

            for(Map.Entry<String, AbstractDouble> row : this.TCP.get(key).entrySet()){

                builder.append(row.getKey()+" = "+row.getValue());

                builder.append(" - ");
            }

            builder.delete(builder.length() - 2, builder.length());

            builder.append("\n");

        }

        return builder.toString();

    }
}
