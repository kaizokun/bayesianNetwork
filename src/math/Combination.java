package math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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

    public static <T> List<List<T>> getSubsets(T[] set) {

        //liste des sous ensembles
        List<List<T>> subSets = new ArrayList((int) Math.pow(2, set.length));
        //ensemble vide
        subSets.add(new ArrayList<>());
        //ensemble complet
        subSets.add(Arrays.asList(set));
        //sous ensemble vide à charger
        LinkedList<T> subSet = new LinkedList<>();
        //pour chaqu etaille de sous ensemble de 1 à taille du set - 1
        for (int s = 1; s <= set.length - 1; s++) {

            loadSubSet(s, subSets, subSet, set, 0);
        }

        return subSets;
    }

    private static <T> void loadSubSet(int size, List<List<T>> subSets, LinkedList<T> subSet, T[] set, int i) {

        //si la taille est arrivé à zero
        if( size == 0 ){
            //on enregistre une copie du sous ensemble créé
            subSets.add(new ArrayList<>(subSet));

            return;
        }
        //pour chaque element restant du set de base
        for( ; i < set.length ; i ++ ){
            //on ajoute l'element
            subSet.addLast(set[i]);
            //on charge la suite en decrementant la taille du sous ensemble
            //à concatener avec le courant, et on passe à l'indice suivant du set d'origine
            loadSubSet(size - 1, subSets, subSet, set, i + 1);
            //on retire l'element pour créer un autre sous ensemble
            subSet.removeLast();
        }

    }
}
