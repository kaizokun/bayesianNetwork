package network.factory;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.MyDoubleFactory;
import environment.*;
import inference.dynamic.Forward;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.dynamic.Model;

import java.util.Arrays;

import static java.util.Arrays.asList;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.MOVE;
import static network.factory.MazeRobotRDDFactory.SIMPLE_MAP_VARS.POSITION;

public class MazeRobotRDDFactory implements NetworkFactory {

    protected SimpleMap simpleMap;

    public MazeRobotRDDFactory(SimpleMap simpleMap) {
        this.simpleMap = simpleMap;
    }

    enum SIMPLE_MAP_VARS implements VarNameEnum {
        POSITION, MOVE, WALL_PERCEPT
    }

    @Override
    public DynamicBayesianNetwork initNetwork() {

        DynamicBayesianNetwork network = new DynamicBayesianNetwork(new MyDoubleFactory());

        network.setForward(new Forward(network));

        //les domaines
        //pour les états des positions x et y
        //pour les actions des directions NSEW
        //pour les percepts des combinaisons de directions NSEW

        IDomain positionsDomain = DomainFactory.getPositionsDomain(this.simpleMap.getAllStates());

        IDomain perceptsDomain = DomainFactory.getMazeWallCaptorDomain();

        IDomain actionsDomain = DomainFactory.getCardinalDomain();

        Variable positionVar = new Variable(POSITION, positionsDomain), actionVar = new Variable(MOVE, actionsDomain);

        //----------------------------------------
        //----------------TEMPS 0 ----------------
        //----------------------------------------

        //la TCP de la variable initiale d'état
        Double[][] state0TCP = initTCPstate0();
        //initialise la tcp à partir des états atteignables dont la probabilité est répartie uniformement
        ProbabilityCompute tcpState0 = network.getTCP(positionsDomain, state0TCP);

        //la variable initiale état position 0
        Variable positionRoot = network.addRootVariable(POSITION, positionsDomain, tcpState0);

        //variable action, ne possede pas de TCP car les probabilités des actions ne sont pas utiles dans ce cas
        //les variables actions sont simplement initialisés pour calculer un nouvel état en fonction d'un etat
        //et action precedente
        Variable action = network.addRootVariable(MOVE, actionsDomain, null);

        //----------------------------------------
        //----------------TEMPS > 0 ----------------
        //----------------------------------------

        //TCP de la variable état aux temps > 0
        //ordre des parents : états precedent et action
        Double[][] stateTCP = initTCPstate(positionsDomain, actionsDomain);

        ProbabilityCompute tcpState = network.getTCP(asList(positionVar, action), positionsDomain, stateTCP);

        //modele extension état
        Model positionExtensionModel = new Model(tcpState);
        //depend des variables états et actions
        positionExtensionModel.addDependencies(positionVar, actionVar);

        network.addTransitionModel(positionVar, positionExtensionModel);

        //modele d'extension vide pour l'action qui na pas de dependences et pas de TCP
        network.addTransitionModel(actionVar, new Model());

        //TCP du modele de capteur
        Double[][] perceptTCP = initTCPpercepts(positionsDomain, perceptsDomain);

        ProbabilityCompute tcpPercept = network.getTCP(asList(positionRoot), perceptsDomain, perceptTCP);

        System.out.println(tcpState0);

        System.out.println(tcpState);

        System.out.println(tcpPercept);

        return null;
    }

    private Double[][] initTCPpercepts(IDomain positionsDomain, IDomain perceptsDomain) {

        Double[][] TCP = new Double[positionsDomain.getSize()][perceptsDomain.getSize()];

        //le percepts correspondant à la position est correct avec une probabilité de 60% (capteur bruité)
        //les autres percepts se partagent les 40% de probabilités restantes
        double probOtherPercepts = 0.4 / perceptsDomain.getValues().size() - 1;

        int d1 = 0;
        //pour chaque position
        for(Domain.DomainValue position1 : positionsDomain.getValues()){

            Position position = (Position) position1.getValue();
            //récupère le percept fourni par la position
            Percept perceptPos = simpleMap.getPercept(position);

            int d2 = 0;

            //pour chaque percept
            for(Domain.DomainValue percept1 : perceptsDomain.getValues()){

                PerceptWall perceptWall = (PerceptWall) percept1.getValue();
                //si le percept correspond à celui forni par l'environnement pour cette position
                if(perceptPos.match(perceptWall)){

                    TCP[d1][d2] = 0.6;

                }else{

                    TCP[d1][d2] = probOtherPercepts;
                }

                d2 ++;
            }

            d1++;
        }

        return TCP;
    }

    private Double[][] initTCPstate0() {
        //pour toutes les positions du labyrinthe sauf les murs
        int totalPositions = this.simpleMap.getAllStates().size();
        int totalPositionsNotFinal = this.simpleMap.getNotFinalStates().size();
        //tableau à deux dimension dont la premier à une entrée
        Double[][] intialStateDistribution = new Double[1][totalPositions];

        int p = 0;
        //pour toutes les positions
        for (Position position : simpleMap.getAllStates()) {
            //si la position est final bonne ou mauvaise sa probabilité est à zero
            //sinon 1 par le nombre de position non finales
            intialStateDistribution[0][p++] = simpleMap.getFinalStates().contains(position) ? 0.0 : 1.0 / totalPositionsNotFinal;
        }

        return intialStateDistribution;
    }

    private Double[][] initTCPstate(IDomain positionsDomain, IDomain actionDomain) {

        Double[][] TCP = new Double[positionsDomain.getSize() * actionDomain.getSize()][positionsDomain.getSize()];

        //deplacement non deterministe ou la probabilite de se deplacer dans la position voulu est de 80% 10% à droite
        //10% à gauche

        int d1 = 0;

        for (Domain.DomainValue<Position> position : positionsDomain.getValues()) {

            for (Domain.DomainValue<Position> action : actionDomain.getValues()) {

                Position currentPos = (Position) position.getValue();
                //tout droit 80%
                Cardinal actionStraight = (Cardinal) action.getValue();
                //à droite 10%
                Cardinal actionRight = actionStraight.getRelativeRight();
                //à gauche 10%
                Cardinal actionLeft = actionStraight.getRelativeLeft();

                Position positionStraight = currentPos.move(actionStraight);
                //si position inatteignable il but sur le mur
                if (!simpleMap.isPositionReachable(positionStraight)) {

                    positionStraight = currentPos;
                }

                Position positionRight = currentPos.move(actionRight);

                if (!simpleMap.isPositionReachable(positionRight)) {

                    positionRight = currentPos;
                }

                Position positionLeft = currentPos.move(actionLeft);

                if (!simpleMap.isPositionReachable(positionLeft)) {

                    positionLeft = currentPos;
                }

                int d2 = 0;

                for (Domain.DomainValue<Position> position1 : positionsDomain.getValues()) {

                    Position positionRs = (Position) position1.getValue();

                    //si la position correspond à celle atteignable en allant tout droit
                    if (positionRs.equals(positionStraight)) {

                        TCP[d1][d2] = 0.8;

                        //si la position correspond à celle atteignable en allant à droite
                    } else if (positionRs.equals(positionRight)) {

                        TCP[d1][d2] = 0.1;

                        //si la position correspond à celle atteignable en allant à gauche
                    } else if (positionRs.equals(positionLeft)) {

                        TCP[d1][d2] = 0.1;

                    } else {

                        TCP[d1][d2] = 0.0;
                    }

                    d2++;
                }

                d1++;
            }

        }

        return TCP;

    }
}
