package agent;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.AbstractDouble;
import environment.Cardinal;
import environment.Maze;
import environment.Percept;
import environment.Position;
import math.Matrix;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.factory.MazeNetworkFactory;

import java.util.*;

import static environment.Cardinal.*;
import static network.factory.MazeNetworkFactory.MAZE_NETWORK_VARS.*;

public class MazeRobot {

    protected DynamicBayesianNetwork dbnMaze;

    protected Maze maze;

    protected IDomain captorDomain = DomainFactory.getMazeWallCaptorDomain();

    protected Map<Integer, List<PositionProb>> positions = new Hashtable<>();

    protected List<PositionProb> reachablePositions;

    protected Map<Integer, Matrix> positionsDistribs = new Hashtable<>();

    protected LinkedList<Percept> percepts = new LinkedList<>();

    protected LinkedList<Cardinal> moves = new LinkedList<>();

    protected LinkedList<Cardinal> movesTest;// = new LinkedList<>(Arrays.asList(EAST, SOUTH, EAST, EAST, WEST, WEST, NORTH, NORTH));

    protected Random random = new Random();

    protected MazeNetworkFactory mazeNetworkFactory;

    //minimum à 0.1 ou un peu moins, si on utilise le même MMC avec toutes les positions accessibles tout le long du processus
    //permet de trouver la position au bout d'un cour laps de temps entre 200ms et 1 seconde
    //minimum à 0 suffit si on recrée le MMC à partir de la liste des positions possibles non nul
    protected double minProb = 0.0;

    public MazeRobot(Maze maze, MazeNetworkFactory mazeNetworkFactory) {

        this.maze = maze;

        this.maze.setRobot(this);

        this.reachablePositions = this.maze.getInitReachablePositions();

        this.positions.put(0, this.reachablePositions);

        this.mazeNetworkFactory = mazeNetworkFactory;

        this.dbnMaze = this.mazeNetworkFactory.initNewNetwork(this.maze, this);
    }

    public void lookUpPosition() {

        //récupere le percept à partir de l'environnement
        Percept percept = this.maze.getPercept();
        //crée une variable observation pour etendre le reseau
        Variable observation = new Variable(CAPTOR_POSITION, captorDomain, percept);

        this.percepts.addLast(percept);

        this.dbnMaze.extend(observation);
        //récupère le filtrage pour le dernier état
        Matrix positionsDistrib = this.dbnMaze.getLastForward().getValue();
        // System.out.println(positionsDistrib);
        this.positionsDistribs.put(dbnMaze.getTime(), positionsDistrib);

        //récupère les positions offrant la plus grande probabilité pour affichage
        this.positions.put(dbnMaze.getTime(), getMostProbablePositions(positionsDistrib));
    }

    public void move() {

        Cardinal randomDirection;

        if(movesTest == null) {
            System.out.println("RANDOM MOVE");
            //récupère toutes les directions
            Set<Cardinal> reachableDirections = Cardinal.getCardinalSetCopy();
            //retirer celles qui ne sont pas accessible
            reachableDirections.removeAll((Collection<?>) this.percepts.getLast().getValue());
            //genere un nombre aléatoire en 0 et le nombre de directions licites
            int rdmId = random.nextInt(reachableDirections.size());

            randomDirection = new ArrayList<>(reachableDirections).get(rdmId);

        }else {
            System.out.println("MOVE TEST");
            randomDirection = movesTest.removeFirst();
        }

        this.moves.add(randomDirection);

        this.maze.moveRobot(randomDirection);
    }

    public boolean positionKnown() {

        return this.positions.get(this.dbnMaze.getTime()).size() == 1;
    }

    private List<PositionProb> getMostProbablePositions(Matrix positionsDistrib) {

        List<PositionProb> mostProbablePositions = new ArrayList<>();

        AbstractDouble minProb = dbnMaze.getDoubleFactory().getNew(this.minProb);
        //pour chaque ligne de la matrice ici la megavariable ne contient qu'une sous variable
        int iRow = 0;

        for (Domain.DomainValue row : positionsDistrib.getRowValues()) {

            Position position = (Position) row.getValue();

            AbstractDouble prob = positionsDistrib.getValue(iRow);

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
        //si aucune on recommence depuis le debut
        //peut arriver si on supprimme les positions qui ont une faible probabilité
        //plutot qu'uniquement celles qui sont à zero.
        if (lastKnownPositions.isEmpty()) {

            this.reachablePositions = this.maze.getInitReachablePositions();

        } else {
            //recuperer toutes les positions alentours
            List<PositionProb> newInitPositions = maze.getNewReachablePosition(lastKnownPositions);
            //enregistre ces positions afin de reinitialiser le mmc
            this.reachablePositions = newInitPositions;
        }

        this.dbnMaze = this.mazeNetworkFactory.initNewNetwork(this.maze, this, dbnMaze.getTime());

    }

    public static class PositionProb {

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
            return position + " : " + prob;
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

    public List<PositionProb> getReachablePositions() {

        return reachablePositions;
    }

    public List<PositionProb> getLastKnowPositions() {

        return this.positions.get(dbnMaze.getTime());
    }

    public void setDbnMaze(DynamicBayesianNetwork dbnMaze) {

        this.dbnMaze = dbnMaze;
    }

    public DynamicBayesianNetwork getDbnMaze() {
        return dbnMaze;
    }

    public void setMinProb(double minProb) {

        this.minProb = minProb;
    }

    public void setMovesTest(LinkedList<Cardinal> movesTest) {
        System.out.println("SET MOVES "+movesTest);
        this.movesTest = movesTest;
    }
}
