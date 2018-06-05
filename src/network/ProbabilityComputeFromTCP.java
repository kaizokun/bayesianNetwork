package network;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class ProbabilityComputeFromTCP implements ProbabilityCompute{

    protected Hashtable<String, Hashtable<Domain.DomainValue, AbstractDouble>> TCP;

    protected AbstractDoubleFactory doubleFactory;

    public ProbabilityComputeFromTCP( IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory){

        this(new ArrayList<>(), varDom, entries, doubleFactory);
    }

    public ProbabilityComputeFromTCP(List<Variable> dependencies, IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory){

        this.doubleFactory = doubleFactory;

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

            Hashtable<Domain.DomainValue, AbstractDouble> row = new Hashtable<>();

            //pour chaque valeur du domaine de la variable
            //on enregistre la probabilité
            //la liste des entrées doit correspondre avec l'ordre des valeurs du domaine de la variable
            for(int j = 0 ; j < varDom.getSize() ; j ++ ){

                //si une chaine est trop longue pour une valeur
                //il faudrait mieux encapsuler les valeur de domaines dans une classe
                //qui contiendrait la aleur sous forme d'objet et un alias plus court
                //pour l'indexation
                Domain.DomainValue domainValue = (Domain.DomainValue) varDom.getValue(j);

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


    private String getDependenciesKey(List<Variable> dependencies){

        //création de la clé correspondant à la combinaison de valeur des parents
        List<String> keyParts = new LinkedList<>();

        for(Variable dep : dependencies){

            keyParts.add(dep.getValue().toString());
        }

        return getDependenciesValuesKey(keyParts);
    }

    @Override
    public AbstractDouble getProbability( Variable var) {

        String depKey = getDependenciesKey(var.dependencies);

        Domain.DomainValue value = var.getDomainValue();

        return this.TCP.get(depKey).get(value);
    }

    public void initRandomValue(Variable var){

        String depKey = getDependenciesKey(var.dependencies);
        //récupere la distribution de la variable pour l'assignation courante des parents
        Map<Domain.DomainValue, AbstractDouble> distrib = this.TCP.get(depKey);
        //total initialisé à zero correspond à un seuil à atteindre
        //à ce total est ajouté chaque frequence pour une valeur de la distribution
        //dans l'odre au fur et à mesure
        //exemple avec une distribution a = 0.2, b = 0.6, c = 0.2
        //si on genere àléatoirement un nombre entre [0; 0.2] ou [0.8; 1]
        //ces chances sont identiques et on à trois fois plus de chance de generer un nombre entre [0.2; 0.8]
        // par exemple on genere 0.6 : 0.2 ne passe pas mais 0.8 (0.6 + 0.2) passe
        // par exemple on genere 0.9 : 0.8 ne passe pas mais 1 (0.2 + 0.2 + 0.6) passe

        //exemple avec une distribution a = 0.33, b = 0.33, c = 0.33
        //chances identique spour chaque valeur
        //en generant un nombre < 0.33 on obtient a
        //[0.33, 0.66] on obtient b sinon c

        AbstractDouble total = doubleFactory.getNew(0.0);

        AbstractDouble rdm = doubleFactory.getNew(new Random().nextDouble());

        for(Map.Entry<Domain.DomainValue, AbstractDouble> entry : distrib.entrySet()){

            total = total.add(entry.getValue());

            if(total.compareTo(rdm) >= 0 ){

                var.setDomainValue(entry.getKey());

                return;
            }
        }
    }

    @Override
    public String toString() {

        StringBuilder  builder = new StringBuilder();

        for(String key : this.TCP.keySet()){

            if(!key.isEmpty()) {

                builder.append(key);

                builder.append(" : ");
            }

            for(Map.Entry<Domain.DomainValue, AbstractDouble> row : this.TCP.get(key).entrySet()){

                builder.append(row.getKey()+" = "+row.getValue());

                builder.append(" - ");
            }

            builder.delete(builder.length() - 2, builder.length());

            builder.append("\n");

        }

        return builder.toString();

    }
}
