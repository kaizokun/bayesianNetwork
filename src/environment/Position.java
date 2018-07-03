package environment;

import java.util.Objects;

public class Position {

    protected int x, y;

    public Position(int y, int x) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean adjacent(Position p2) {

        for(Cardinal cardinal : Cardinal.values()){

            Position adj = move(cardinal);

            if(p2.equals(adj)){

                return true;
            }
        }

        return false;
    }

    public Position move(Cardinal cardinal){

        return new Position(this.y + cardinal.getDeltaY(), this.x + cardinal.getDeltaX());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x == position.x &&
                y == position.y;
    }

    @Override
    public int hashCode() {

        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "("+y+", "+x+")";
    }
}
