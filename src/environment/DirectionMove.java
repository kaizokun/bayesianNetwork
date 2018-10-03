package environment;

//implementation plu ssimple directement dans le sens des aiguilles d'une montre
//avec possibilité de sur place plutot qu'avec Cardinal qui necessite une table de conversion car
//modifier pourrait poser problèmes...
public enum DirectionMove implements Action {

    NORTH, EAST, SOUTH, WEST, ON_THE_SPOT;

    private static final int delta[][] = new int[][]{{+1, 0}, {0, +1}, {-1, 0}, {0, -1}, {0, 0}};

    private static final DirectionMove[] moves = new DirectionMove[]{NORTH, EAST, SOUTH, WEST};

    private static final DirectionMove[] oppositeMoves = new DirectionMove[]{SOUTH, WEST, NORTH, EAST};

    public int getDeltaY() {

        return delta[this.ordinal()][0];
    }

    public int getDeltaX() {

        return delta[this.ordinal()][1];
    }

    public boolean isLoop(DirectionMove lastAction) {

        return oppositeMoves[ordinal()] == lastAction;
    }

    public DirectionMove getRelativeRight() {

        if (this.equals(ON_THE_SPOT)) {

            return ON_THE_SPOT;
        }

        int idDirection = (ordinal() + 1) % (values().length - 1);

        return values()[idDirection];
    }

    public DirectionMove getRelativeLeft() {

        if (this.equals(ON_THE_SPOT)) {

            return ON_THE_SPOT;
        }

        int idDirection = ordinal() - 1 < 0 ? (DirectionMove.values().length - 2) : ordinal() - 1;

        return values()[idDirection];
    }

    public static DirectionMove[] getMoves() {
        return moves;
    }
}
