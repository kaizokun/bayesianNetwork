package domain;

import environment.Cardinals;
import environment.Position;

public class DomainFactory {

    public static IDomain getBooleanDomain() {

        return new Domain(1, 0);
    }

    public static IDomain getABCDDomain() {

        return new Domain('a', 'b', 'c', 'd');
    }

    public static IDomain getMazePositionDomain(int xLimit, int yLimit) {

        Position[] positions = new Position[xLimit * yLimit];

        int xy = 0;

        for (int x = 0; x < xLimit; x++) {

            for (int y = 0; y < yLimit; y++) {

                positions[xy] = new Position(x, y);

                xy++;
            }
        }

        return new Domain(positions);
    }

    public static IDomain getCardinalsDomain() {

        return new Domain(Cardinals.NORTH, Cardinals.SOUTH, Cardinals.EAST, Cardinals.WEST);
    }

}
