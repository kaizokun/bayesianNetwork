package math;

import java.util.*;

public class Combination {


    /*
     * sous ensemble de percepts :
     *
     * Creer le sous ensemble vide
     *
     * Creer le sous ensemble contenant tout les percepts
     *
     * Pour size allant de 1 à 3
     *
     *     Creer un sous ensemble de taille size ( sous ensemble vide, sous ensembles, iCardinalite = 0 )
     *
     * Fin Pour
     *
     * Creer un sous ensemble de taille size ( sous ensemble vide, sous ensembles  )
     *
     * Si la taille est zero
     *
     *      ajouter une copie du sous ensemble dans la liste completes
     *
     * Fin Si
     *
     *  Pour chaque cardinalité en partant de iCardinalité à totalCardinalité - 1
     *
     *      ajouter la premiere cardinalité au sous ensemble
     *
     *      Creer un sous ensemble de taille size - 1 ( sous ensemble vide, sous ensembles, iCardinalité )
     *
     *      retirer la cardinalité
     *
     *  Fin Pour
     *
     * */

    public static class Count {

        public int count = 0;
    }

    public static <T> List<T>[] getSubsets(T[] set) {

        Count c = new Count();

        //liste des sous ensembles soit 2 exposant la taille de l'ensemble
        List<T>[] subSets = new List[(int) Math.pow(2, set.length)];
        //ensemble vide
        subSets[c.count++] = new ArrayList<>();
        //ensemble complet
        subSets[c.count++] = Arrays.asList(set);
        //sous ensemble vide à charger
        LinkedList<T> subSet = new LinkedList<>();
        //pour chaque taille de sous ensemble de 1 à taille du set - 1
        for (int s = 1; s <= set.length - 1; s++) {

            loadSubSet(s, subSets, subSet, set, 0, c);
        }

        return subSets;
    }

    private static <T> void loadSubSet(int size, List<T>[] subSets, LinkedList<T> subSet, T[] set, int i, Count c) {

        //si la taille est arrivé à zero
        if (size == 0) {
            //on enregistre une copie du sous ensemble créé
            subSets[c.count++] = new ArrayList<>(subSet);

            return;
        }
        //pour chaque element restant du set de base
        for (; i < set.length; i++) {
            //on ajoute l'element
            subSet.addLast(set[i]);
            //on charge la suite en decrementant la taille du sous ensemble
            //à concatener avec le courant, et on passe à l'indice suivant du set d'origine
            loadSubSet(size - 1, subSets, subSet, set, i + 1, c);
            //on retire l'element pour créer un autre sous ensemble
            subSet.removeLast();
        }

    }

    public static <T> List<List<T>> getCombinations(List<List<T>> valuesLists) {

        int combinationSize = 1;

        for (List<T> values : valuesLists) {

            combinationSize *= values.size();
        }

        List<List<T>> combinationsList = new ArrayList<>(combinationSize);

        List<T> combination = (List<T>) Arrays.asList(new Object[valuesLists.size()]);

        loadCombinations(valuesLists, combinationsList, combination, 0);

        return combinationsList;
    }

    private static <T> void loadCombinations(List<List<T>> valuesLists, List<List<T>> combinationsList, List<T> combination, int iList) {

        if (iList == valuesLists.size()) {

            combinationsList.add(new ArrayList<>(combination));

            return;
        }

        List<T> values = valuesLists.get(iList);

        for (T value : values) {

            combination.set(iList, value);

            loadCombinations(valuesLists, combinationsList, combination, iList + 1);
        }
    }

    private static int callCount;

    public static long countDerangementsDynamic(int n) {

        //callCount = 0;

        Map<Integer, Long> saveRs = new Hashtable<>();

        saveRs.put(2, 1L);

        saveRs.put(1, 0L);

        long rs = countDerangementsDynamic(n, saveRs);

        //System.out.println("CALL COUNT "+callCount);

        return rs;
    }

    private static long countDerangementsDynamic(int n, Map<Integer, Long> saveRs) {

        Long savedRs = saveRs.get(n);

        if (savedRs != null) {

            return savedRs;
        }

        //callCount ++;

        long rs = (n - 1) * (countDerangementsDynamic(n - 1, saveRs) + countDerangementsDynamic(n - 2, saveRs));

        saveRs.put(n, rs);

        return rs;
    }

    public static long countDerangements(int n) {

        //callCount = 0;

        long rs = countDerangementsRec(n);

        //System.out.println("CALL COUNT "+callCount);

        return rs;
    }

    public static long countDerangementsRec(int n) {

        if (n == 2) {

            return 1;

        } else if (n == 1) {

            return 0;
        }

       // callCount ++;

        return (n - 1) * (countDerangementsRec(n - 1) + countDerangementsRec(n - 2));
    }

    public static List<List<List>> loadAssemblements(List<List> lists, int n) {

        // EXEMPLE
        // Avec les assemblements possible de 3 listes de pieces différentes  {1,2}, {a,b} et {+,-}
        // on commence par transformer la premier liste afin d'obtenir une liste d'assemblement atomiques
        // on demarre donc avec un ensemble de possibilités uniques {{1},{2}} à associer avec une liste d'items {a,b}
        // on associera d'abord les possibilités avec les permutations de la liste suivante ici {a,b}
        // à chaque permutation de {a,b} on peut combiner les possibilités courante
        // soit {a,b} combiné avec {{1},{2}} on obtient {{1,a},{2,b}}
        // puis {b,a} on obtient {{1,b},{2,a}}
        // on se retrouve avec un nouvel ensemble de possibilités d'assemblements {{1,a},{2,b}}, {{1,b},{2,a}}
        // ! {1,a} ou {2,b} tout comme {1} ou {2} precedemment sont, considérés comme des pieces uniques après ajout
        // d'autres pieces.
        // on enchaine avec la prochaine liste {+,-}, même principe
        // soit {+,-} combiné avec cette fois deux possibilités {{1,a},{2,b}}, {{1,b},{2,a}}
        // on obtiens :  {{1,a,+},{2,b,-}}, {{1,b,+},{2,a,-}}
        // puis {-,+} combiné avec les mêmes possibilités {{1,a},{2,b}}, {{1,b},{2,a}}
        // on obtiens :  {{1,a,-},{2,b,+}}, {{1,b,-},{2,a,+}}
        // soit 4 possibilités  {{1,a,+},{2,b,-}}, {{1,b,+},{2,a,-}}, {{1,a,-},{2,b,+}}, {{1,b,-},{2,a,+}}
        // en ainsi de suite ...

        // resultat : 1ere dimension, les possibilités
        //           2eme les assemblements de n pieces pour une possibilité
        //           3eme les pieces d'un assemblement
        // List est un pièce partiellement ou entierrement assemblée
        List<List<List>> possibilities = new LinkedList<>();
        //autant d'assemblement qu'il y a de pieces dans chaque liste soit n
        List<List> assemblements = new ArrayList<>(n);
        //possibilité de depart
        possibilities.add(assemblements);
        //chaque assemblement contient une piece de la liste
        for (Object item : lists.get(0)) {
            //assemblement atomique d'un item
            List assemblement = new ArrayList<>(1);
            //ajout de l'item
            assemblement.add(item);
            //ajout de l'assemblement à la possibilité unique de départ
            assemblements.add(assemblement);
        }
        //items à ignorer dans la liste d'items à ajouter pour la génération des permutation
        //par defaut tous à faux
        boolean[] ignoreItems = new boolean[n];
        //pour chaque liste suivante
        for (int l = 1; l < lists.size(); l++) {

            //prochaine liste de possibilités
            List<List<List>> nextPossibilities = new LinkedList<>();
            //on associe les possibilités courantes aux permutation de la liste d'items courant
            createAssemblements(possibilities, nextPossibilities, lists.get(l), ignoreItems, Arrays.asList(new Object[n]), 0);
            //les nouvelles possibilités remplace les precedentes pour les combiner à la liste d'items suivante
            possibilities = nextPossibilities;
        }

        return possibilities;
    }

    /**
     * @param possibilites     liste des possibilités d'assemblements pour un ensemble de pieces
     * @param nextPossibilites liste de possibilités suivante
     * @param items            liste d'items à combiner aux possibilités courantes
     * @param ignoreItems      indice des items à ignorer lors de la création d'une permutation de la liste d'items
     * @param permutation      permutation en cour de création
     * @param i                indice courant de la permutation où ajouter un item
     */
    private static void createAssemblements(List<List<List>> possibilites, List<List<List>> nextPossibilites,
                                            List items, boolean[] ignoreItems, List permutation, int i) {
        //permutation complete
        if (i == items.size()) {

            //chaque possibilité d'assemblements
            for (List<List> assemblements : possibilites) {

                int a = 0;
                //liste de nouveaux assemblements en combinant un assemblement à une piece
                List<List> newAssemblements = new ArrayList<>(items.size());
                //pour chaque chaque assemblement, le nombre doit être identique aux nombre d'items
                for (List assemblement : assemblements) {
                    //crée une nouvelle liste de pieces absorbant un assemblement,
                    //taille d'un assemblement precedent + 1
                    List newAssemblement = new ArrayList<>(assemblement.size() + 1);

                    newAssemblement.addAll(assemblement);
                    //ajoute l'item situé à la position 'a' pour la permutation courante
                    newAssemblement.add(permutation.get(a));
                    //ajout à la liste des assemblements (possibilité)
                    newAssemblements.add(newAssemblement);
                    //prochain item et assemblement
                    a++;
                }

                //ajout de la nouvelle possibilité à la prochaine liste
                nextPossibilites.add(newAssemblements);
            }

            return;
        }

        //création d'une permutation
        //pour chaque item
        for (int it = 0; it < items.size(); it++) {
            //si l'iem n'a pas encore été ajouté
            if (!ignoreItems[it]) {
                //on l'ajoute à la permutation en cour de création
                permutation.set(i, items.get(it));
                //on indique que litem est à ignorer
                ignoreItems[it] = true;
                //on enchaine avec la suite des items
                createAssemblements(possibilites, nextPossibilites, items, ignoreItems, permutation, i + 1);
                //on le demarque à ignorer
                ignoreItems[it] = false;
            }
        }

    }

    public static double factorial(int n) {

        if (n <= 1) {

            return 1;
        }

        return n * factorial(n - 1);
    }


}
