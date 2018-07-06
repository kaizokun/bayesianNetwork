package inference.dynamic.mmc;

import math.Matrix;
import network.dynamic.MMC;

import java.util.Hashtable;
import java.util.Map;

public class SmoothingForwardDecBackwardIncMMC extends SmoothingMMC {

    protected SmoothingForwardDecMMC smoothingForwardDec;

    public SmoothingForwardDecBackwardIncMMC(MMC mmc, ForwardMMC forwardMMC, BackwardMMC backwardMMC) {

        super(mmc, forwardMMC, backwardMMC);

        this.smoothingForwardDec = new SmoothingForwardDecMMC(mmc, forwardMMC, backwardMMC);
    }

    @Override
    public Map<Integer, SmoothingMatrices> smoothing(int timeStart, int timeEnd) {

        //System.out.println("SMOOTHING FORWARD INCREMENTAL BACKWARD DECREMENTAL");

        //si timeStart == timeEnd on lisse sur un seul état
        //en général on fera le lissage sur une certain nombre d'états compris entre [time - n; time - 1]
        //le range pourrait aussi être [time - n; time - x]
        //il faut donc verifier si 'time - n' est supérieur ou égal à 1 si inférieur il vaut 1
        //ensuite faire de même pour time - x qui cette fois si il est inférieur à 1 on ne fait rien
        //il n'y a rien à lisser...
        //la premier fois que l'on effectue le smoothing on va utiliser smoothingConstant si possible
        //et enregistrer le backward et le foward ainsi que la multiplication des deux, le lissage,
        //donc au début il se peut que en fonction de la plage que l'on souhaite lisser on le fasse pour 0 à 1 états
        //à parti du moment ou on la fait pour 1 état sauvegardé soit le dernier de la plage que l'on a pu obtenir
        //on peut incrementer son forward et son backward pour faire avance le dernier etat de la plage
        //puis les decrementer pour calculer les precedents et obtenir la plage complete mise à jour.

        //au depart on lissera sur une plage de longeur [1,1] qui pourrait augmenter par la suite

        //verifie si le smoothing existe déja pour le temps precedent
        if (mmc.getSmoothings().containsKey(timeEnd - 1)) {

            //on crée une nouvelle Map pour enregistrer la nouvelle plage de smoothing
            Map<Integer, SmoothingMatrices> newSmoothings = new Hashtable<>();

            //on a effectué un lissage pour la coupe precedente
            //on recupere le dernier état lissé au timeEnd precedent
            SmoothingMatrices smoothingMatrices = mmc.getSmoothings().get(timeEnd - 1);
            //c'est celui ci dont on va incrementer le forward ainsi que le backward
            //pour calculer le lissage de l'état en timeEnd
            Matrix timeEndForward = this.forwardMMC.incrementForward(timeEnd, smoothingMatrices.forward);

            Matrix timeEndBackward = this.backwardMMC.incrementBackward(timeEnd, mmc.getTime(), smoothingMatrices.backward);

            SmoothingMatrices newSmoothingMatrices = new SmoothingMatrices(timeEndForward,
                    timeEndBackward,
                    timeEndForward.multiplyRows(timeEndBackward).normalize(),
                    timeEnd);
            //on sauvegarde le dernier
            newSmoothings.put(timeEnd, newSmoothingMatrices);

            //on initialise le forward et le backward courant
            //maintenant pour chaque time allant de timeEnd - 1 à timeStart
            //on decremente le backward courant
            //on recupere le forward enregistré pour un precedent smoothing à un temps time
            //si il n'existe pas on decremente le forward courant
            //on enregistre le smoothing pour time

            timeEnd--;

            while (timeEnd >= timeStart) {
                //on tente de recuperer un forward enregistré precedemment pour une même variable à un temps identique
                timeEndForward = mmc.getSmoothings().get(timeEnd).getForward();
                //si il n'existe pas encore
                if (timeEndForward == null) {

                    timeEndForward = this.forwardMMC.decrementForward(timeEnd, timeEndForward);
                }

                timeEndBackward = this.backwardMMC.decrementBackward(timeEnd, timeEndBackward);

                newSmoothingMatrices = new SmoothingMatrices(timeEndForward, timeEndBackward, timeEndForward.multiplyRows(timeEndBackward).normalize(), timeEnd);

                newSmoothings.put(timeEnd, newSmoothingMatrices);

                timeEnd--;
            }

            return newSmoothings;

        } else {
            //cas de base ou aucun lissage n'a été effectué on va l'appliquer pour la premier fois
            return this.smoothingForwardDec.smoothing(timeStart, timeEnd);
        }

    }


}
