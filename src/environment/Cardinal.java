package environment;


import java.util.LinkedHashSet;

import java.util.Set;

public enum Cardinal implements Action {

    NORTH, SOUTH, EAST, WEST;

    private static final int delta[][] = new int[][]{{+1, 0}, {-1, 0}, {0, +1}, {0, -1}};

    private static final Set<Cardinal> cardinalSet = new LinkedHashSet<>();

    private static final Cardinal[] clockOrderValues = new Cardinal[4];

    private static final int[] clockOrderOrdinal = new int[4];

    static {

        cardinalSet.add(NORTH);

        cardinalSet.add(SOUTH);

        cardinalSet.add(EAST);

        cardinalSet.add(WEST);

        clockOrderValues[0] = NORTH;

        clockOrderValues[1] = EAST;

        clockOrderValues[2] = SOUTH;

        clockOrderValues[3] = WEST;

        clockOrderOrdinal[NORTH.ordinal()] = 0;

        clockOrderOrdinal[EAST.ordinal()] = 1;

        clockOrderOrdinal[SOUTH.ordinal()] = 2;

        clockOrderOrdinal[WEST.ordinal()] = 3;
    }

    public static Set<Cardinal> getCardinalSet() {

        return cardinalSet;
    }

    public static Cardinal[] getCardinalTab() {

        return values();
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

    private static Cardinal[] getClockOrderValues() {
        return clockOrderValues;
    }

    public Cardinal getRelativeRight() {

        int idDirection = (ordinalClock() + 1) % values().length;

        return getClockOrderValues()[idDirection];
    }

    public Cardinal getRelativeLeft() {

        int idDirection = ordinalClock() - 1 < 0 ? Cardinal.values().length - 1 : ordinalClock() - 1;

        return getClockOrderValues()[idDirection];
    }

    private int ordinalClock() {

        return clockOrderOrdinal[this.ordinal()];
    }
}
