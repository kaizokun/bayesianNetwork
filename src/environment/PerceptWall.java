package environment;

import math.Combination;

import java.util.*;

public class PerceptWall implements Percept<PerceptWall> {

    protected Set<Cardinal> walls = new LinkedHashSet<>();

    public PerceptWall() { }

    public void addWallDirection(Cardinal cardinal){

        walls.add(cardinal);
    }

    public PerceptWall(List<Cardinal> walls) {

        this.walls.addAll(walls);
    }

    public static Percept[] getAllPercepts(){

        List<Cardinal>[] subSets = Combination.getSubsets(Cardinal.values());

        Percept[] percepts = new Percept[subSets.length];

        int s = 0;

        for(List<Cardinal> subSet : subSets){

            percepts[s] = new PerceptWall(subSet);

            s++;
        }

        return percepts;
    }

    @Override
    public boolean match(PerceptWall percept){

        if (percept.walls.size() != this.walls.size()) {

            return false;
        }

        for (Cardinal wall : percept.walls) {

            if (!this.walls.contains(wall)) {

                return false;
            }
        }

        return true;
    }

    @Override
    public Object getValue() {

        return this.walls;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerceptWall that = (PerceptWall) o;
        return this.match(that);
    }

    @Override
    public int hashCode() {

        return Objects.hash(walls);
    }

    @Override
    public String toString() {
        return walls.toString() ;
    }
}
