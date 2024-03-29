package inference.dynamic;

import domain.data.AbstractDouble;
import math.Distribution;
import math.Matrix;
import network.MegaVariable;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;

import java.util.*;

import static inference.dynamic.Util.*;

public class Smoothing {

    protected DynamicBayesianNetwork network;

    protected Forward forward;

    protected Backward backward;

    public Smoothing(DynamicBayesianNetwork network) {

        this.network = network;

        this.forward = new Forward(network);

        this.backward = new Backward(network);
    }

    protected Distribution smootDistribution(Distribution forward, Distribution backward) {
        //normalisation de la distribution forward
        Distribution forwardNormal = new Distribution(forward).normalize();
        //suppression des valeurs totales
        Distribution smootDistrib = backward.multiply(forwardNormal);

        return smootDistrib;
    }

    public AbstractDouble smoothing(Variable request) {

        return this.smoothing(request, this.network.getTime(), 1);
    }

    public AbstractDouble smoothing(Variable request, int markovOrder) {

        return this.smoothing(request, this.network.getTime(), markovOrder);
    }

    public AbstractDouble smoothing(List<Variable> requests) {

        return this.smoothing(requests, network.getTime(), 1);
    }

    public AbstractDouble smoothing(List<Variable> requests, int markovOrder) {

        return this.smoothing(requests, network.getTime(), markovOrder);
    }

    public AbstractDouble smoothing(Variable request, int timeEnd, int markovOrder) {

        return this.smoothing(Arrays.asList(new Variable[]{request}), timeEnd, markovOrder);
    }

    protected AbstractDouble smoothing(List<Variable> requests, int timeEnd, int markovOrder) {

        Variable megaRequest = requests.size() == 1 ? requests.get(0) : network.encapsulate(requests);

        String key = getDistribSavedKey(requests);

        this.forward.forward(requests, key, 0, true);

        if (markovOrder > 1) {

            this.backward.backward(requests, key, 0, timeEnd, markovOrder, true);

        } else {

            this.backward.backward(requests, key, 0, timeEnd, true);
        }

        Distribution distributionFinal =
                this.smootDistribution(this.forward.forwardMatrices.get(key), this.backward.backwardMatrices.get(key));

        return distributionFinal.get(megaRequest.getDomainValue()).divide(distributionFinal.getTotal());
    }


    public void forwardBackward(List<Variable> requests) {

        this.forwardBackward(requests, 0, this.network.getTime(), this.network.getTime());
    }

    public void forwardBackward(List<Variable> requests, int smootStart, int smootEnd) {

        this.forwardBackward(requests, smootStart, smootEnd, this.network.getTime());
    }

    public void forwardBackward(List<Variable> requests, int smootStart, int smootEnd, int backWardEnd) {

        /*
         * smootStart : debut de l'intervalle pour les variable à lisser
         * smootEnd : fin de l'intervalle
         *
         * exemple : on souhaite lisser entre 5 et 8 avec un temps courant de 10
         *
         * il faut calculer le forward sur l'intervalle 0 à 5 le forward etant initialisé au temps 0
         * et calculer le backward sur l'intervalle 10 à 5 le backward etant initialisé au temps final
         * bien qu'il pourrait l'être depuis un temps précédent ici backWardEnd.
         *
         * */

        //on travaille sur les variables de la requete situées au temps smootEnd
        List<Variable> startForwardVars = new LinkedList<>();
        //variables situées au temps T
        for (Variable req : requests) {

            startForwardVars.add(this.network.getVariable(smootEnd, req));
        }
        //on travaille sur les variables de la requete situées au temps smootStart
        List<Variable> startBackwardVars = new LinkedList<>();

        //variables situées au temps 1
        for (Variable req : requests) {

            startBackwardVars.add(this.network.getVariable(smootStart, req));
        }
        String forwardKey = getDistribSavedKey(startForwardVars);

        String backwardKey = getDistribSavedKey(startBackwardVars);

        this.forward.forward(startForwardVars, forwardKey, 0, true);

        this.backward.backward(startBackwardVars, backwardKey, 0, backWardEnd, true);

        Map<String, Matrix> forwardDistributionsNormal = new Hashtable<>();

        Map<String, Matrix> smootDistributions = new Hashtable<>();

        Map<String, Matrix> smootDistributionsNormal = new Hashtable<>();

        this.forward.showForward();

        this.backward.showBackward();

        for (int time = smootStart; time <= smootEnd; time++) {

            String keyForward = getDistribSavedKey(requests, time);

            String keyBackWard = getDistribSavedKey(requests, time);

            Distribution smootDistrib = this.smootDistribution(
                    this.forward.forwardMatrices.get(keyForward),
                    this.backward.backwardMatrices.get(keyBackWard));

            smootDistributions.put(keyForward, smootDistrib);

            smootDistributionsNormal.put(keyForward, new Distribution(smootDistrib).normalize());

            forwardDistributionsNormal.put(keyForward, new Distribution(this.forward.forwardMatrices.get(keyForward)).normalize());
        }

        System.out.println("FORWARD NORMAL " + forwardDistributionsNormal);

        System.out.println("FORWARDxBACKWARD " + smootDistributions);

        System.out.println("FORWARDxBACKWARD NORMAL " + smootDistributionsNormal);
    }

    public void forwardBackward(Variable request) {

        this.forwardBackward(request, 0, this.network.getTime(), this.network.getTime());
    }

    public void forwardBackward(Variable request, int smootStart, int smootEnd) {

        this.forwardBackward(request, smootStart, smootEnd, this.network.getTime());
    }

    public void forwardBackward(Variable request, int smootStart, int smootEnd, int backWardEnd) {

        forwardBackward(new LinkedList<Variable>(Arrays.asList(new Variable[]{request})),
                smootStart, smootEnd, backWardEnd);
    }

}
