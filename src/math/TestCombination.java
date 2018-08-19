package math;


import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;

public class TestCombination {

    @Test
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

}
