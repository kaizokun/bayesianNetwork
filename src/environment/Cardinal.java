package environment;

public enum Cardinal {

    NORTH, SOUTH, EAST, WEST;

    private static final int delta[][] = new int[][]{{1, 0}, {-1, 0}, {0, -1}, {0, 1}};

    public int[] getDeltaYX(){

        return delta[this.ordinal()];
    }

    public int getDeltaY(){

        return delta[this.ordinal()][0];
    }

    public int getDeltaX(){

        return delta[this.ordinal()][1];
    }

}
