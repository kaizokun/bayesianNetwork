package agent;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDouble;
import environment.Cardinal;
import environment.Maze;
import environment.Position;
import inference.dynamic.mmc.SmoothingMMC;
import math.Matrix;
import network.BayesianNetworkFactory;
import network.Variable;
import network.dynamic.MMC;

import java.util.*;

import static network.BayesianNetworkFactory.MAZE_NETWORK_VARS.CAPTOR_POSITION;

public class MazeRobot {

    protected MMC mazeMMC;

    protected Maze maze;

    protected IDomain captorDomain = DomainFactory.getMazeWallCaptorDomain();

    protected Map<Integer, List<PositionProb>> positions = new Hashtable<>();

    protected Map<Integer, Matrix> positionsDistribs = new Hashtable<>();

    protected LinkedList<List<Cardinal>> percepts = new LinkedList<>();

    protected LinkedList<Cardinal> moves = new LinkedList<>();

    //protected LinkedList<Cardinal> movesTest = new LinkedList<>(Arrays.asList(new Cardinal[]{Cardinal.WEST, Cardinal.EAST, Cardinal.WEST}));

    protected Random random = new Random();

    //minimum à 0.1 ou un peu moins, si on utilise le même MMC avec toutes les positions accessibles tout le long du processus
    //permet de trouver la position au bout d'un cour laps de temps entre 200ms et 1 seconde
    //minimum à 0 suffit si on recrée le MMC à partir de la liste des positions possibles non nul
    protected double minProb = 0 ;

    public MazeRobot(MMC mazeMMC, Maze maze) {

        this.mazeMMC = mazeMMC;

        this.maze = maze;

        this.maze.setRobot(this);
    }

    public void lookUpPosition() {

        //récupere le percept à partir de l'environnement
        List<Cardinal> percept = new ArrayList<>(this.maze.getPercept());
        //crée une variable observation pour etendre le reseau
        Variable observation = new Variable(CAPTOR_POSITION, captorDomain, percept);

        this.percepts.addLast(percept);

        this.mazeMMC.extend(observation);
        //récupère le filtrage pour le dernier état
        Matrix positionsDistrib = this.mazeMMC.getLastForward().getValue();

        this.positionsDistribs.put(mazeMMC.getTime(), positionsDistrib);

        //récupère les positions offrant la plus grande probabilité pour affichage
        this.positions.put(mazeMMC.getTime(), getMostProbablePositions(positionsDistrib));
    }

    public void move() {

        //récupère toutes les directions
        Set<Cardinal> reachableDirections = Cardinal.getCardinalSetCopy();
        //retirer celles qui ne sont pas accessible
        reachableDirections.removeAll(this.percepts.getLast());
        //genere un nombre aléatoire en 0 et le nombre de directions licites
        int rdmId = random.nextInt(reachableDirections.size());

        Cardinal randomDirection = new ArrayList<>(reachableDirections).get(rdmId);

        //randomDirection = movesTest.removeFirst();

        this.moves.add(randomDirection);

        this.maze.moveRobot(randomDirection);
    }

    public boolean positionKnown() {

        return this.positions.get(this.mazeMMC.getTime()).size() == 1;
    }

    private List<PositionProb> getMostProbablePositions(Matrix positionsDistrib) {

        List<PositionProb> mostProbablePositions = new ArrayList<>();

        AbstractDouble minProb = mazeMMC.getDoubleFactory().getNew(this.minProb);
        //pour chaque ligne de la matrice ici la megavariable ne contient qu'une sous variable
        int iRow = 0;

        for (List<Domain.DomainValue> row : positionsDistrib.getRowValues()) {

            Position position = (Position) row.get(0).getValue();

            AbstractDouble prob = positionsDistrib.getValue(iRow, 0);

            int cmp = prob.compareTo(minProb);

            if (cmp > 0) {

                mostProbablePositions.add(new PositionProb(position, prob));
            }

            iRow++;
        }

        return mostProbablePositions;
    }

    public void reload() {

        //récupération les dernieres positions possibles du robot
        List<PositionProb> lastKnownPositions = getLastKnowPositions();
        //recuperer toutes les positions alentours
        List<PositionProb> newInitPositions = maze.getNewReachablePosition(lastKnownPositions);
        //enregistre ces positions dans le labyrinthe
        //afin de reinitialiser un mmc avec ces positions
        maze.setReachablePositions(newInitPositions);

        SmoothingMMC smoothingMMC = mazeMMC.getSmoothingMMC();

        mazeMMC = BayesianNetworkFactory.getMazeMMC(maze, mazeMMC.getTime());

        mazeMMC.setSmoothingMMC(smoothingMMC);
    }

    public static class PositionProb{

        protected Position position;

        protected AbstractDouble prob;

        public PositionProb(Position position, AbstractDouble prob) {
            this.position = position;
            this.prob = prob;
        }

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }

        public AbstractDouble getProb() {
            return prob;
        }

        public void setProb(AbstractDouble prob) {
            this.prob = prob;
        }

        @Override
        public String toString() {
            return  position +" : "+ prob;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PositionProb that = (PositionProb) o;
            return Objects.equals(position, that.position);
        }

        @Override
        public int hashCode() {

            return Objects.hash(position);
        }
    }

    public Map<Integer, List<PositionProb>> getPositions() {
        return positions;
    }

    public Map<Integer, Matrix> getPositionsDistribs() {
        return positionsDistribs;
    }

    public LinkedList<Cardinal> getMoves() {

        return moves;
    }

    public List<PositionProb> getLastKnowPositions() {

        return this.positions.get(mazeMMC.getTime());
    }

    public void setMazeMMC(MMC mazeMMC) {

        this.mazeMMC = mazeMMC;
    }

    public void setMinProb(double minProb) {

        this.minProb = minProb;
    }
}
