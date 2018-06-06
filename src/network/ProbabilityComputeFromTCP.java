package network;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class ProbabilityComputeFromTCP implements ProbabilityCompute {

    protected Hashtable<String, Hashtable<Domain.DomainValue, AbstractDouble>> TCP;

    protected Hashtable<String, List<Map.Entry<Domain.DomainValue, FrequencyRange>>> cumulativeFrequences;

    protected AbstractDoubleFactory doubleFactory;

    public ProbabilityComputeFromTCP(IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory) {

        this(new ArrayList<>(), varDom, entries, doubleFactory);
    }

    public ProbabilityComputeFromTCP(List<Variable> dependencies, IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory) {

        this.doubleFactory = doubleFactory;

        this.TCP = new Hashtable<>();

        this.initCTP(dependencies, varDom, entries, new LinkedList<>(), 0, doubleFactory);

        this.cumulativeFrequences = new Hashtable<>();

        this.initCulumativeFrequencies(varDom);
    }


    private void initCTP(List<Variable> dependencies, IDomain varDom, Double[][] entries, LinkedList<String> keyParts,
                         int iDep, AbstractDoubleFactory doubleFactory) {

        //Double[][] entries : valeur de la TCP : la premier dimension correspond à une combinaison de valeur pour les parents
        //la deuxiemme correspond aux probabilités pour les valeurs du domaine de la variable


        //quand le dernier parent a été atteind on à une combinaison de valeur pour chaque parent
        //formant la clé d'une entrée ou ligne de la TCP
        if (iDep == dependencies.size()) {

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
            for (int j = 0; j < varDom.getSize(); j++) {

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

        for (Domain.DomainValue o : dependencieDomain.getValues()) {

            keyParts.addLast(o.getValue().toString());

            this.initCTP(dependencies, varDom, entries, keyParts, iDep + 1, doubleFactory);

            keyParts.removeLast();
        }
    }

    private void initCulumativeFrequencies(IDomain varDom) {

        //pour chaque entrée de première dimension de la TCP
        for (Map.Entry<String, Hashtable<Domain.DomainValue, AbstractDouble>> entry : this.TCP.entrySet()) {

            //frequences cumulées pour une combinaison de valeurs parents
            AbstractDouble cumul = doubleFactory.getNew(0.0);
            //crée un tableau de frequence cumulées de la taille du domaine
            List<Map.Entry<Domain.DomainValue, FrequencyRange>> frequencies = new ArrayList(Arrays.asList(new Map.Entry[varDom.getSize()]));

            int i = 0;
            //pour chaque entrée de deuxieme dimension de la TCP
            //soit chaque frequence pour une valeur du domaine de la variable en fonction d'une combinaison de valeur parents
            for (Map.Entry<Domain.DomainValue, AbstractDouble> freq : entry.getValue().entrySet()) {
                //on additionne la frequence
                FrequencyRange range = new FrequencyRange();

                range.min = cumul;

                cumul = cumul.add(freq.getValue());

                range.max = cumul;

                Map.Entry<Domain.DomainValue, FrequencyRange> rangeEntry = new AbstractMap.SimpleEntry(freq.getKey(), range);

                frequencies.set(i, rangeEntry);

                i++;
            }

            this.cumulativeFrequences.put(entry.getKey(), frequencies);
        }
    }


    /**
     * Génere une clé à partir d'une combinaison de valeurs pour les variables parents
     **/
    private String getDependenciesValuesKey(List<String> keyParts) {

        StringBuilder key = new StringBuilder();

        for (String keyPart : keyParts) {

            key.append(keyPart);

            key.append("-");
        }

        if (key.length() != 0) {

            key.deleteCharAt(key.length() - 1);
        }

        return key.toString();
    }

    private String getDependenciesKey(List<Variable> dependencies) {

        //création de la clé correspondant à la combinaison de valeur des parents
        List<String> keyParts = new LinkedList<>();

        for (Variable dep : dependencies) {

            keyParts.add(dep.getValue().toString());
        }

        return getDependenciesValuesKey(keyParts);
    }

    @Override
    public AbstractDouble getProbability(Variable var) {

        String depKey = getDependenciesKey(var.dependencies);

        Domain.DomainValue value = var.getDomainValue();

        return this.TCP.get(depKey).get(value);
    }

   // boolean initPrint = false;

    @Override
    public Domain.DomainValue getRandomValue(Variable var) {

        String depKey = getDependenciesKey(var.dependencies);
        //récupere la distribution de la variable pour l'assignation courante des parents
        List<Map.Entry<Domain.DomainValue, FrequencyRange>> rangevalues = this.cumulativeFrequences.get(depKey);
        //total initialisé à zero correspond à un seuil à atteindre
        //à ce total est ajouté chaque frequence pour une valeur de la distribution dans l'ordre
        /*
         * exemple pour une distribution impaire a = 10, b = 40, c = 30, d = 20
         * 0 <= a < 10 <= b < 50 <= c < 80 <= d < 100
         * exemple pour une distribution a = 10, b = 50, c = 40
         * en cumulant les frequences on obtient 0 <= a < 10 <= b < 60 <= c < 100
         * en générant un nombre A aléatoire entre 0 et 100
         * par exemple on a 50 % d'obtenir un nombre entre 10 et 60
         * 10 % d'obtenir un nombre entre 0 et 10
         * 40 % entre 60 et 100
         * ce qui correspond aux chances d'obtenir les différentes valeurs
         *
         * on peut parcourir sequentiellement le tableau des frequences cumulées
         * jusqu'à atteindre le seuil.
         * ou calculer à l'avance et faire une recherche dichotomique ce qui serait plus efficace
         * */

        //AbstractDouble total = doubleFactory.getNew(0.0);

        return this.dichotomicSearch(rangevalues,  doubleFactory.getNew(new Random().nextDouble()), 0, rangevalues.size());

    }

    private Domain.DomainValue dichotomicSearch(List<Map.Entry<Domain.DomainValue, FrequencyRange>> rangeEntries, AbstractDouble search, int s, int e) {

        int middle = s + ((e - s) / 2);

        Map.Entry<Domain.DomainValue, FrequencyRange> rangeEntry = rangeEntries.get(middle);

        switch (rangeEntry.getValue().compareTo(search)) {
            case 0:

                return rangeEntry.getKey();

            case -1:

                return dichotomicSearch(rangeEntries, search, s, middle);

            default:

                return dichotomicSearch(rangeEntries, search, middle + 1, e);
        }
    }

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        for (String key : this.TCP.keySet()) {

            if (!key.isEmpty()) {

                builder.append(key);

                builder.append(" : ");
            }

            for (Map.Entry<Domain.DomainValue, AbstractDouble> row : this.TCP.get(key).entrySet()) {

                builder.append(row.getKey() + " = " + row.getValue());

                builder.append(" - ");
            }

            builder.delete(builder.length() - 2, builder.length());

            builder.append("\n");

        }

        return builder.toString();
    }

    private class FrequencyRange {

        private AbstractDouble min, max;

        public FrequencyRange() {
        }

        public FrequencyRange(AbstractDouble min, AbstractDouble max) {

            this.min = min;

            this.max = max;
        }

        public int compareTo(AbstractDouble search) {

            //inférieur au minimum rang inférieur
            if (search.compareTo(min) < 0) {

                return -1;
            }

            //inférieur au minimum rang supérieur
            if (search.compareTo(max) > 0) {

                return 1;
            }

            //range[0,1]
            //rdm [0,0.99999]
            //supérieur ou égal au min et inférieur au max dans le range
            //return search.compareTo(min) >= 0 && search.compareTo(max) < 0;

            return 0;
        }
    }


}
