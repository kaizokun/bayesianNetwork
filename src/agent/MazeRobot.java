package agent;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDouble;
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

    protected List<Position> positions = new ArrayList<>();

    protected LinkedList<Cardinal> moves = new LinkedList<>();

    protected Random random = new Random();

    protected double minProb = 0.1;

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
        //récupère le filtrage pour le dernier état
        Matrix positionsDistrib = this.mazeMMC.getLastForward().getValue();
        //récupère les positions offrant la plus grande probabilité pour affichage
        List<Position> mostProbablePositions = getMostProbablePositions(positionsDistrib);

        this.positions = mostProbablePositions;

        //System.out.println(positionsDistrib);

        System.out.println(positions);

        //récupère toutes les directions
        Set<Cardinal> reachableDirections = Cardinal.getCardinalSetCopy();
        //retirer celles qui ne sont pas accessible
        reachableDirections.removeAll(percept);
        //genere un nombre aléatoire en 0 et le nombre de directions licites
        int rdmId = random.nextInt(reachableDirections.size());

        Cardinal randomDirection = new ArrayList<>(reachableDirections).get(rdmId);

        moves.add(randomDirection);

        this.maze.moveRobot(randomDirection);

    }

    public boolean positionKnown() {

        if (this.positions.size() == 1){

            Position pos = this.positions.get(0);

            this.positions.clear();

            this.positions.add(pos.move(this.moves.getLast()));

            return true;
        }

        return false;
    }


    private List<Position> getMostProbablePositions(Matrix positionsDistrib) {

        List<Position> mostProbablePositions = new ArrayList<>();

        AbstractDouble minProb = mazeMMC.getDoubleFactory().getNew(this.minProb);
        //pour chaque ligne de la matrice ici la megavariable ne contient qu'une sous variable
        int iRow = 0;

        for (List<Domain.DomainValue> row : positionsDistrib.getRowValues()) {

            Position position = (Position) row.get(0).getValue();

            AbstractDouble prob = positionsDistrib.getValue(iRow, 0);

            int cmp = prob.compareTo(minProb);

            if (cmp > 0) {

                mostProbablePositions.add(position);
            }

            iRow++;
        }

        return mostProbablePositions;
    }


    public List<Position> getPositions() {

        return positions;
    }

    public List<Cardinal> getMoves() {

        return moves;
    }

}
