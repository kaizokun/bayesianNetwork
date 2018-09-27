package network.factory;

import domain.Domain;
import domain.DomainFactory;
import domain.IDomain;
import domain.data.MyDoubleFactory;
import environment.Cardinal;
import environment.Position;
import environment.SimpleMap;
import inference.dynamic.Forward;
import network.ProbabilityCompute;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.Arrays;
import java.util.Set;

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

        for (int d1 = 0; d1 < stateTCP.length; d1++) {

            for (int d2 = 0; d2 < stateTCP[d1].length; d2++) {

                System.out.print("[" + stateTCP[d1][d2] + "]");

            }

            System.out.println();
        }

        ProbabilityCompute tcpState1 = network.getTCP(Arrays.asList(positionRoot, action), positionsDomain, state0TCP);

        System.out.println(tcpState0);

        System.out.println(tcpState1);

        return null;
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

               //System.out.println("POSITION : " + position + " ACTION : " + action);

                Position currentPos = (Position) position.getValue();
                //tout droit 80%
                Cardinal actionStraight = (Cardinal) action.getValue();
                //à droite 10%
                Cardinal actionRight = actionStraight.getRelativeRight();
                //à gauche 10%
                Cardinal actionLeft = actionStraight.getRelativeLeft();

                Position positionStraight = currentPos.move(actionStraight);

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

                //System.out.println(positionStraight + " " + positionLeft + " " + positionRight);

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
/*
                    System.out.println(positionRs+" "+TCP[d1][d2]);
                    System.out.println(positionRs.equals(positionStraight));
                    System.out.println(positionRs.equals(positionLeft));
                    System.out.println(positionRs.equals(positionRight));
                    */
                    d2++;
                }

                d1++;
            }

        }

        return TCP;

    }
}
