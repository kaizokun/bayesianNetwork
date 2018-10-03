package network;

import domain.Domain;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;

import java.util.*;

public class ProbabilityComputeFromTCP implements ProbabilityCompute {

    protected Hashtable<String, Hashtable<Domain.DomainValue, AbstractDouble>> TCP;

    protected Hashtable<String, List<Map.Entry<Domain.DomainValue, FrequencyRange>>> cumulativeFrequencies;

    protected Hashtable<String, List<Map.Entry<Domain.DomainValue, FrequencyRange>>> cumulativeMarkovFrequencies;

    protected AbstractDoubleFactory doubleFactory;

    protected Double[][] matrice;

    protected Random random = new Random();

    protected IDomain varDom;

    protected List<Variable> dependencies;

    private static final char IDX_SEPARATOR = '.';

    public ProbabilityComputeFromTCP(IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory) {

        this(new ArrayList<>(), varDom, entries, doubleFactory);
    }

    public ProbabilityComputeFromTCP(Variable[] dependencies, IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory) {

        this(Arrays.asList(dependencies), varDom, entries, doubleFactory);
    }

    public ProbabilityComputeFromTCP(List<Variable> dependencies, IDomain varDom, Double[][] entries, AbstractDoubleFactory doubleFactory) {

        this.matrice = entries;

        this.doubleFactory = doubleFactory;

        this.TCP = new Hashtable<>();

        this.dependencies = dependencies;

        this.initCTP(dependencies, varDom, entries, Arrays.asList(new Domain.DomainValue[dependencies.size()]), 0, doubleFactory);

        this.cumulativeFrequencies = new Hashtable<>();

        this.varDom = varDom;

        this.initCulumativeFrequencies();
    }

    /*===================== INITIALISATION ==============================*/

    private void initCTP(List<Variable> dependencies, IDomain varDom, Double[][] entries, List<Domain.DomainValue> keyParts,
                         int iDep, AbstractDoubleFactory doubleFactory) {

        //Double[][] entries : valeur de la TCP : la premier dimension correspond à une combinaison de valeur pour les parents
        //la deuxiemme correspond aux probabilités pour les valeurs du domaine de la variable


        //quand le dernier parent a été atteind on à une combinaison de valeur pour chaque parent
        //formant la clé d'une entrée ou ligne de la TCP
        if (iDep == dependencies.size()) {

            //genere la clé sous forme de chaine de charactère à partir des valeurs
            // String key = this.getDependenciesValuesKey(keyParts);

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
                //qui contiendrait la valeur sous forme d'objet et un alias plus court
                //pour l'indexation
                Domain.DomainValue domainValue = varDom.getDomainValue(j);

                AbstractDouble prob = doubleFactory.getNew(entries[iRow][j]);

                row.put(domainValue, prob);
            }

            //ajout de la ligne dans la TCP
            this.TCP.put(getValuesKey(keyParts), row);

            return;
        }

        Variable variable = dependencies.get(iDep);

        IDomain dependencieDomain = variable.getDomain();

        for (Domain.DomainValue value : dependencieDomain.getValues()) {

            keyParts.set(iDep, value);

            this.initCTP(dependencies, varDom, entries, keyParts, iDep + 1, doubleFactory);
        }
    }

    @Override
    public void initCulumativeFrequencies() {

        initCulumativeFrequencies(this.varDom);
    }

    private void initCulumativeFrequencies(IDomain varDom) {

        //pour chaque entrée de première dimension de la TCP
        for (Map.Entry<String, Hashtable<Domain.DomainValue, AbstractDouble>> entry : this.TCP.entrySet()) {

            //frequences cumulées pour une combinaison de valeurs parents
            AbstractDouble cumul = doubleFactory.getNew(0.0);
            //crée un tableau de frequence cumulées de la taille du domaine
            List<Map.Entry<Domain.DomainValue, FrequencyRange>> frequencies = Arrays.asList(new Map.Entry[varDom.getSize()]);

            int i = 0;
            //pour chaque entrée de deuxieme dimension de la TCP
            //soit chaque frequence pour une valeur du domaine de la variable en fonction d'une combinaison de valeur parents
            for (Map.Entry<Domain.DomainValue, AbstractDouble> freq : entry.getValue().entrySet()) {
                //on additionne la frequence
                FrequencyRange range = new FrequencyRange();

                range.setMin(cumul);

                cumul = cumul.add(freq.getValue());

                range.setMax(cumul);

                Map.Entry<Domain.DomainValue, FrequencyRange> rangeEntry = new AbstractMap.SimpleEntry(freq.getKey(), range);

                frequencies.set(i, rangeEntry);

                i++;
            }

            this.cumulativeFrequencies.put(entry.getKey(), frequencies);
        }
    }

    @Override
    public void initCumulativeMarkovFrequencies(Variable variable) {

        this.cumulativeMarkovFrequencies = new Hashtable<>();
        //couverture de markov sans la variable (et les observations)
        List<Variable> markovCoverTab = new ArrayList<>(variable.markovKover);

        this.initCumulativeMarkovFrequencies(markovCoverTab, variable, 0);
    }

    private void initCumulativeMarkovFrequencies(List<Variable> markovCovertab, Variable variable, int iVar) {

        //derniere variable de la couverture initialisée
        if (iVar == markovCovertab.size()) {

            //disribution de la variable pour chacune de ses valeur en fonction d'un etat de la couverture
            List<Map.Entry<Domain.DomainValue, AbstractDouble>> valuesProbs = new LinkedList<>();

            AbstractDouble totalProb = doubleFactory.getNew(0.0);
            //pour chaque valeur du domaine
            for (Domain.DomainValue domainValue : variable.getDomainValues()) {

                variable.setDomainValue(domainValue);
                //prob initialisé à 1
                AbstractDouble markovProb = doubleFactory.getNew(1.0);
                //multiplie la probabilité de la variable en fonction des parents
                markovProb = markovProb.multiply(variable.getProbability());

                //multiplie la probabilité de chaque enfants en fonction des parents
                for (Variable child : variable.children) {

                    markovProb = markovProb.multiply(child.getProbability());
                }
                //ajoute au total : pour ensuite calculer sur 100%
                totalProb = totalProb.add(markovProb);

                valuesProbs.add(new AbstractMap.SimpleEntry<>(domainValue, markovProb));
            }
            //récupere la clé correspondant à l'état de la couverture
            String markovKey = this.getDependenciesKey(markovCovertab);

            //tableau de frequences cumulées pour la distribution de la variable en fonction de l etat de la couverture
            List<Map.Entry<Domain.DomainValue, FrequencyRange>> cumulProbs = new ArrayList<>();

            this.cumulativeMarkovFrequencies.put(markovKey, cumulProbs);
            //le minimum du range demarre à zéro
            AbstractDouble min = doubleFactory.getNew(0.0);

            for (Map.Entry<Domain.DomainValue, AbstractDouble> valueProb : valuesProbs) {

                FrequencyRange range = new FrequencyRange();

                range.setMin(min);
                //probabilité pour une valeur en rapport avec le total de toute les valeur pour attaindre les 100%
                AbstractDouble max = min.add(valueProb.getValue().divide(totalProb));

                range.setMax(max);
                //le min devient le max du prochain range
                min = max;

                cumulProbs.add(new AbstractMap.SimpleEntry(valueProb.getKey(), range));
            }

            return;
        }

        //récupere une variable
        Variable markovVar = markovCovertab.get(iVar);
        //pour chaque valeur du domaine initialiser la variable
        for (Domain.DomainValue domainValue : markovVar.getDomainValues()) {

            markovVar.setDomainValue(domainValue);

            initCumulativeMarkovFrequencies(markovCovertab, variable, iVar + 1);
        }
    }

    /**------------------ PRIVATES ----------------*/

    /**
     * Génere une clé à partir d'une combinaison de valeurs pour les variables parents
     **/

    private String getDependenciesKey(Collection<Variable> dependencies) {

        //création de la clé correspondant à la combinaison de valeur des parents
        StringBuilder builder = new StringBuilder();

        for (Variable dep : dependencies) {
            //ajoute l'id de domain de la valeur de la variable
            //plus court...
            builder.append(dep.getDomainValue().getIndex());

            builder.append(IDX_SEPARATOR);
        }

        return builder.toString();
    }

    public String getValuesKey(Collection<Domain.DomainValue> values) {

        //création de la clé correspondant à la combinaison de valeur des parents
        StringBuilder builder = new StringBuilder();

        for (Domain.DomainValue value : values) {
            //ajoute l'id de domaine de la valeur de la variable
            //plus court...
            //on pourrait aussi se baser sur l'index dans les méthodes hascode et equals de la classe DomainValue
            builder.append(value.getIndex());

            builder.append(IDX_SEPARATOR);
        }

        return builder.toString();
    }

    @Override
    public String getValueKey(Domain.DomainValue value) {

        return value.getIndex()+""+IDX_SEPARATOR;
    }
    /**
     * ------------------ GETTER PROBABILITIES AND VALUES ----------------
     */

    @Override
    public AbstractDouble getProbability(Variable var) {

        String depKey = getDependenciesKey(var.dependencies);

        Domain.DomainValue value = var.getDomainValue();

        return this.TCP.get(depKey).get(value);
    }

    @Override
    public Domain.DomainValue getRandomValue(Variable var) {

        String depKey = getDependenciesKey(var.dependencies);
        //récupere la distribution de la variable pour l'assignation courante des parents
        List<Map.Entry<Domain.DomainValue, FrequencyRange>> rangevalues = this.cumulativeFrequencies.get(depKey);
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

        return FrequencyRange.dichotomicSearch(rangevalues, doubleFactory.getNew(this.random.nextDouble()));
    }

    @Override
    public Domain.DomainValue getRandomValueFromMarkovCover(Variable variable) {

        AbstractDouble rdm = doubleFactory.getNew(this.random.nextDouble());

        List<Map.Entry<Domain.DomainValue, FrequencyRange>> cumulFreq = this.cumulativeMarkovFrequencies.get(this.getDependenciesKey(variable.markovKover));

        return FrequencyRange.dichotomicSearch(cumulFreq, rdm);
    }

    @Override
    public int size() {
        return this.TCP.size();
    }

    /**
     * -------------------------AFFICHAGES--------------------------
     */

    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder();

        builder.append("\n");

        List<String> sortedKeys = new ArrayList<>(this.TCP.keySet());

        Collections.sort(sortedKeys);
        //clés d'une des ligne de la TCP identique pour chaque ligne...
        List<Domain.DomainValue> sortedKeyD2 = new ArrayList<>(this.TCP.values().iterator().next().keySet());

        Comparator<Domain.DomainValue> cmp = new Comparator<Domain.DomainValue>() {
            @Override
            public int compare(Domain.DomainValue o1, Domain.DomainValue o2) {
                return o1.getValue().toString().compareTo(o2.toString());
            }
        };

        Collections.sort(sortedKeyD2, cmp);

        for (String key : sortedKeys) {

            if (!key.isEmpty()) {
                //indexes des valeurs de domaine parent
                //echapper le . car charactere speciales dans les regex
                String idxs[] = key.split("\\" + IDX_SEPARATOR);

                int d = 0;
                //pour chaque dependence
                for (Variable dep : dependencies) {

                    //recupere l'index sous forme d'entier
                    int idx = Integer.parseInt(idxs[d]);
                    //recupere la valeur de domaine (DOMAINVALUE) à partir de l'index
                    Domain.DomainValue domainValue = dep.getDomain().getDomainValue(idx);

                    builder.append(domainValue.toString());

                    builder.append(".");

                    d++;
                }
                //on retire le dernier point pour l'estetique
                builder.deleteCharAt(builder.length() - 1);

                builder.append(" : ");
            }

            Map<Domain.DomainValue, AbstractDouble> row = this.TCP.get(key);

            for (Domain.DomainValue colKey : sortedKeyD2) {

                builder.append(colKey + " = " + row.get(colKey));

                builder.append(" - ");
            }

            builder.delete(builder.length() - 2, builder.length());

            builder.append("\n");

        }

        return builder.toString();
    }

    @Override
    public void showCumulativeMarkovFrequencies() {

        for (Map.Entry<String, List<Map.Entry<Domain.DomainValue, FrequencyRange>>> entry : this.cumulativeMarkovFrequencies.entrySet()) {

            System.out.println();
            System.out.println("=====KEY==== " + entry.getKey());

            for (Map.Entry<Domain.DomainValue, FrequencyRange> entry2 : entry.getValue()) {

                System.out.println(entry2.getKey() + " : " + entry2.getValue());
            }
        }
    }


    /**
     * ------------------------- ACCESSORS-------------------------
     */

    @Override
    public AbstractDoubleFactory getDoubleFactory() {
        return doubleFactory;
    }

    @Override
    public Hashtable<String, Hashtable<Domain.DomainValue, AbstractDouble>> getTCP() {
        return TCP;
    }
}
