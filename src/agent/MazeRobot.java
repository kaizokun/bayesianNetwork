package agent;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDouble;
import domain.data.AbstractDoubleFactory;
import environment.Cardinal;
import environment.Maze;
import environment.Position;
import math.Matrix;
import network.Variable;
import network.dynamic.MMC;

import java.util.*;

import static network.BayesianNetworkFactory.MAZE_NETWORK_VARS.CAPTOR_POSITION;

public class MazeRobot {

    protected MMC mazeMMC;

    protected Maze maze;

    protected IDomain captorDomain = DomainFactory.getMazeWallCaptorDomain();

    protected Matrix positionsDistrib;

    public MazeRobot(MMC mazeMMC, Maze maze) {

        this.mazeMMC = mazeMMC;

        this.maze = maze;

        this.maze.setRobot(this);
    }

    public void nextStep() {

        //récupere le percept à partir de l'environnement
        List<Cardinal> percept = new ArrayList<>(this.maze.getPercept());
        //crée une variable observation pour etendre le reseau
        Variable observation = new Variable(CAPTOR_POSITION, captorDomain, percept);

        this.mazeMMC.extend(observation);

        //ystem.out.println(this.mazeMMC);
        //récupère le filtrage pour le dernier état
        this.positionsDistrib = this.mazeMMC.getLastForward().getValue();

        System.out.println(this.positionsDistrib);

        //récupère toutes les directions
        Set<Cardinal> reachableDirections = Cardinal.getCardinalSetCopy();
        //retirer celles qui ne sont pas accessible
        reachableDirections.removeAll(percept);

        System.out.println(getMostProbablePostions(mazeMMC.getDoubleFactory()));
    }

    private List<Map.Entry<Position, AbstractDouble>> getMostProbablePostions(AbstractDoubleFactory doubleFactory) {

        List<Map.Entry<Position, AbstractDouble>> maxPositions = new LinkedList<>();

        AbstractDouble maxProb = doubleFactory.getNew(0.0);
        //pour chaque ligne de la matrice ici la megavariable ne contient qu'une sous variable
        int iRow = 0;

        for (List<Domain.DomainValue> row : this.positionsDistrib.getRowValues()) {

            Position position = (Position) row.get(0).getValue();

            AbstractDouble prob = this.positionsDistrib.getValue(iRow, 0);

            int cmp = prob.compareTo(maxProb);

            if (cmp > 0) {

                maxProb = prob;

                maxPositions = new LinkedList<>();

                maxPositions.add(new AbstractMap.SimpleEntry<>(position, prob));

            } else if (cmp == 0) {

                maxPositions.add(new AbstractMap.SimpleEntry<>(position, prob));
            }

            iRow++;
        }

        return maxPositions;
    }

}
