package inference;

import domain.Domain;
import domain.data.AbstractDouble;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.dynamic.Model;

import java.util.*;

public class ParticulateFiltering {

    public static void ask(DynamicBayesianNetwork network, List<Variable> states, List<Variable> observations, int totalSamples) {

        //ici le but est de calculer N echantillons pour une coupe temporelles pour toutes les variable d'états

        //recuperation rapide des variables d'états lors de la création de nouvelles variables pour la coupe suivante
        //pour l'assignation des dependences
        Map<Variable, Variable> statesMap = new Hashtable<>();

        for (Variable state : states) {
            //recupere la variable du reseau à la coupe temporelle courante
            //necessite de stocker les variables dans le reseau ici on aura besoin
            //uniquement des variables de la derniere coupe temporelle
            statesMap.put(state, network.getVariable(network.getTime(), state));
        }

        //chaque sample est une map liant une variable est une valeur de domaine
        //pour ensuite pouvoir recuperer rapidement la valeur d'une ou plusieurs variable parent dans le sample precedent
        //les echantillons seront stockés dans le reseau bayesien dynamique pour l'appel suivant à la fin de la procédure

        List<Map<Variable, Domain.DomainValue>> samples = new ArrayList<>(totalSamples);

        AbstractDouble weighSamples[] = new AbstractDouble[totalSamples];

        //si le temps est zéro on echantillone à partir de la TCP de la coupe temporelle 0
        if (network.getTime() == 0) {

            for (int s = 0; s < totalSamples; s++) {
                //initialise des valeurs echantillonées en fonction des TCP des variables
                Map<Variable, Domain.DomainValue> sample = new Hashtable<>();
                //on echantillone chaque variable du reseau à la coupe temporelle courante
                //et on enregistre la valeur echantillonée pour la variable dans la map
                for (Variable state : statesMap.values()) {

                    state.initRdmValue();

                    sample.put(state, state.getDomainValue());

                    samples.add(s, sample);
                }
            }

            logSample(samples, totalSamples, weighSamples);
        }

        //crée les variables états pour la coupe temporelle suivante
        Map<Variable, Variable> nextStatesMap = new Hashtable<>(states.size());

        for (Variable state : states) {
            //recuperer le modele d' extension pour la variable
            List<Model> models = network.getVarTransitionModels(state);
            //ici le dernier pour un modele de markov d'ordre 1
            Model model = models.get(models.size() - 1);
            //liste des dependances de la variable état
            List<Variable> dependencies = new ArrayList<>();
            //pour chaque dependence définie dans le modèle
            for (Model.Dependency dependency : model.getDependencies()) {

                dependencies.add(statesMap.get(dependency.getDependency()));
            }
            //initialisation de la nouvelle variable état
            Variable nextState = new Variable(state.getLabel(), state.getDomain(), model.getProbabilityCompute(),
                    dependencies, network.getTime() + 1);

            nextStatesMap.put(nextState, nextState);
        }

        //crée les variables observations pour la coupe temporelle suivante
        Map<Variable, Variable> nextObsMap = new Hashtable<>(observations.size());

        //dependences
        //pour chaque observation créer une variable d'observation complete avec ses dependences états
        for (Variable obs : observations) {
            //recuperer le modele de capteurs
            List<Model> models = network.getVarCaptorModels(obs);

            Model model = models.get(models.size() - 1);
            //liste des dependances de la variable observation
            List<Variable> dependencies = new ArrayList<>();
            //pour chaque dependence définie dans le modèle
            for (Model.Dependency dependency : model.getDependencies()) {

                dependencies.add(nextStatesMap.get(dependency.getDependency()));
            }
            //initialisation de la nouvelle variable observation
            Variable nextObs = new Variable(obs.getLabel(), obs.getDomain(), model.getProbabilityCompute(),
                    dependencies, network.getTime() + 1);
            //attribue la valeur
            nextObs.setDomainValue(obs.getDomainValue());

            nextObsMap.put(nextObs, nextObs);
        }

        for (int s = 0; s < totalSamples; s++) {

            //generer un echantillon pour la coupe temporelle suivante à partir de l'echantillon courant
            //pour chaque nouvelle variable on se contente d'assigner les valeur parent pour chaque echantillon
            //et d'echantilloner la valeur de la variable

            Map<Variable, Domain.DomainValue> sample = samples.get(s);

            Map<Variable, Domain.DomainValue> nextSample = new Hashtable<>();
            //pour chaque variable de la requete à la coupe suivante
            for (Variable nextState : nextStatesMap.values()) {
                //assigne à ses parents les valeurs du sample courant
                for (Variable dependency : nextState.getDependencies()) {

                    dependency.setDomainValue(sample.get(dependency));
                }
                //initialise une valeur aléatoire en fonction des valeurs parents
                nextState.initRdmValue();

                nextSample.put(nextState, nextState.getDomainValue());
            }

            samples.set(s, nextSample);

            AbstractDouble weigh = network.getDoubleFactory().getNew(1.0);
            //pour chaque variable d'observation
            for(Variable nextObs : nextObsMap.values()){
                //attribue à ses variables parents les valeurs du sample s nouvelement crée
                for(Variable dependency : nextObs.getDependencies()){

                    dependency.setDomainValue(nextSample.get(dependency));
                }
                //multiplie les probabilités des observations
                weigh = weigh.multiply( nextObs.getProbability() );
            }

            weighSamples[s] = weigh;
        }

        logSample(samples, totalSamples, weighSamples);

        //reechantillonage
        //jusqu'à atteindre un total de n echantillon
        //choisir au hazard un element du tableau et en fonction du poids le selectionner ou non
        //pour cela on genere un nombre aléatoire entre 0 et 1 si ce nombre est inférier ou égal au poids
        //on selectionne l'echantillon sinon pas
        //crée une nouvelle liste de samples car certains precedents peuvent être selectionnés plusieurs fois
        List<Map<Variable, Domain.DomainValue>> newSamples = new ArrayList<>(totalSamples);

        Random rdm = new Random();

        int cpt = 0;

        for(int s = 0 ; s < totalSamples ; cpt ++){
            //genere un indice de sample aléatoire
            int i = rdm.nextInt(totalSamples);
            //récupère le poids associé
            AbstractDouble rdmWeigh = weighSamples[i];
            //génére un nombre entre 0 et 1 non compris
            AbstractDouble limit = network.getDoubleFactory().getNew(rdm.nextDouble());
            //si la limite est inférieur ou égal au poids le sample est choisi
            //Par exemple un poids de 0.2 à autant de chance
            //d'être selectionné que la probabilité de générer un nombre entre
            //0 et 0.2 qui est approximativement de 20 %
            if( limit.compareTo(rdmWeigh) <= 0 ){
                //on remplace
                newSamples.add(samples.get(i));

                s ++;
            }
        }

        logSample(newSamples, totalSamples, weighSamples);

        System.out.println(cpt);
    }

    private static void logSample(List<Map<Variable, Domain.DomainValue>> samples, int totalSamples, AbstractDouble[] weigh){

        double totalTrue = 0, totalFalse = 0;

        int s = 0 ;

        for(Map<Variable, Domain.DomainValue> sample : samples){

            //System.out.println(sample+" "+weigh[s]);

            if(sample.values().iterator().next().getValue().equals(1)){

                totalTrue ++;
            }else{

                totalFalse ++;
            }

            s ++;
        }

        System.out.println((totalTrue/totalSamples)+" "+(totalFalse/totalSamples));
    }
}
