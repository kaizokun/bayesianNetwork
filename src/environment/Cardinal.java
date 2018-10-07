package environment;


import java.util.LinkedHashSet;

import java.util.Set;

public enum Cardinal implements Action {

    NORTH, SOUTH, EAST, WEST;

    private static final int delta[][] = new int[][]{{+1, 0}, {-1, 0}, {0, +1}, {0, -1}};

    private static final Set<Cardinal> cardinalSet = new LinkedHashSet<>();


    static {

        cardinalSet.add(NORTH);

        cardinalSet.add(SOUTH);

        cardinalSet.add(EAST);

        cardinalSet.add(WEST);

    }

    public static Set<Cardinal> getCardinalSetCopy() {

        return new LinkedHashSet<>(cardinalSet);
    }

    public int getDeltaY() {

        return delta[this.ordinal()][0];
    }

    public int getDeltaX() {

        return delta[this.ordinal()][1];
    }


}
