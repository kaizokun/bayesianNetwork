package util;

import java.util.LinkedList;

public class ListUtil {


    public static LinkedList reverseOrder(LinkedList list){

        LinkedList rs = new LinkedList();

        for(Object o : list){

            rs.addFirst(o);
        }

        return rs;
    }

}
