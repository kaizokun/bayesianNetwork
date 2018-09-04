package math;


import org.junit.Test;

import java.util.*;

import static java.util.Arrays.asList;

public class TestCombination {

    //@Test
    public void assemblementsTest() {

        List<List> itemsList = new LinkedList<>();

        itemsList.add(asList(1, 2, 3));

        itemsList.add(asList('a', 'b', 'c'));

        itemsList.add(asList('+', '-', '*'));

        List<List<List>> possibilities = Combination.loadAssemblements(itemsList, itemsList.get(0).size());

        for (List<List> assemblements : possibilities) {

            System.out.println(assemblements);
        }
    }

    //@Test
    public void assemblementsTest2() {

        List<List> itemsList = new LinkedList<>();

        itemsList.add(asList("e:1", "e:2", "e:3", "e:4", "e:5"));

        itemsList.add(asList("r:1", "r:2", "r:3", "r:4", "r:5"));

        List<List<List>> possibilities = Combination.loadAssemblements(itemsList, itemsList.get(0).size());

        Map<Integer, List<List<List>>> derangementsTab = new Hashtable<>();

        for (List<List> assemblements : possibilities) {

            //nombre de mauvais assemblements on considere bon par defaut
            int totalDerangements = 0;
            //pour chaque assemblement
            for (List assemblement : assemblements) {

                //id d'item bogus
                String lastId = "";
                //pour chaque item de l'assemblement
                for (Object item : assemblement) {
                    //recupere l'item, ici une chaine de charactere
                    String itemStr = (String) item;

                    String[] keyId = itemStr.split(":");
                    //recupere la partie id
                    String id = keyId[1];
                    //si differente de la precedente sauf pour le bogus
                    if (!lastId.equals("") && !id.equals(lastId)) {
                        //on compte un derangement, il suffit que deux pieces d'un asssemblement ne correspondent pas
                        totalDerangements++;
                        break;
                    }

                    lastId = id;
                }
            }

            if (!derangementsTab.containsKey(totalDerangements)) {

                derangementsTab.put(totalDerangements, new LinkedList<>());
            }

            derangementsTab.get(totalDerangements).add(assemblements);
        }

        int c = 0;

        List<Integer> keys = new ArrayList<>(derangementsTab.keySet());

        Collections.sort(keys);

        for (Integer derangements : keys) {

            System.out.println("DERANGEMENTS : " + derangements + " - TOTAL = " + derangementsTab.get(derangements).size());

            System.out.println();

            for (List<List> assemblements : derangementsTab.get(derangements)) {

                System.out.println("[" + c + "]   " + assemblements);

                c++;
            }

            System.out.println();
        }

    }

    private int n = 42;

    @Test
    public void testCountDerangements() {

        System.out.println("TOTAL DERANGEMENTS POUR N = (" + n + ") : " + Combination.countDerangements(n));
    }

    @Test
    public void testCountDerangementsDynamic() {

        System.out.println("TOTAL DERANGEMENTS POUR N = (" + n + ") : " + Combination.countDerangementsDynamic(n));
    }

}
