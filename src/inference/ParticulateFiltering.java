package inference;

import domain.Domain;
import network.MegaVariable;
import network.Variable;
import network.dynamic.DynamicBayesianNetwork;
import network.dynamic.Model;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class ParticulateFiltering {

    public void ask(DynamicBayesianNetwork network, List<Variable> states, List<Variable> observations, int totalSamples) {

        //ici le but est de calculer N echantillons pour une coupe temporelles pour toutes les variable d'états
        //recuperation rapide des variables d'états lors de la création de nouvelles variables pour la coupe suivante
        Map<Variable, Variable> statesMap = new Hashtable<>();

        for (Variable state : states) {
            //recupere la variable du reseau à la coupe temporelle courante
            //necessite de stocker les avriables dans le reseau ici on aura besoin
            //uniquement des variables de la derniere coupe temporelle
            statesMap.put(state, network.getVariable(network.getTime(), state));
        }

        //chaque sample est une map liant une variable est une valeur de domaine
        //pour ensuite pouvoire recuperer rapidement la valeur d'une variable parent dans le sample preccedent
        //les echantillons seront stockés dans le reseau baysien dynamique pour le temps suivant à la fin de la procédure

        List<Map<Variable, Domain.DomainValue>> samples = new ArrayList<>();

        //si le temps est zéro on echantillone à partir de la TCP de la coupe temporelle 0
        if (network.getTime() == 0) {

            //crée une liste des variables états pour la coupe temporelle 0
            List<Variable> networkStates = new ArrayList<>();
            //recupere dans le reseau les etats qui nous interessent
            for (Variable reqVar : states) {

                networkStates.add(network.getVariable(0, reqVar));
            }
            //crée une megavariable à partir de la liste
            for (int s = 0; s < totalSamples; s++) {
                //initialise des valeurs echantillonées en fonction des TCP des variables
                Map<Variable, Domain.DomainValue> sample = new Hashtable<>();
                //on echantillone chaque variable et on enregistre la valeur echantilloné pour la variable dans la map
                for (Variable state : states) {

                    state.initRdmValue();

                    sample.put(state, state.getDomainValue());

                    samples.set(s, sample);
                }
            }
        }

        List<Variable> nextStates = new ArrayList<>(states.size());

        for (Variable state : states) {

            //recuperer le modele d'extension pour la variable
            List<Model> models = network.getVarTransitionModels(state);
            //ici le dernier pour un modele de markov d'ordre 1
            Model model = models.get(models.size() - 1);
            //liste des dependances de la variable
            List<Variable> dependencies = new ArrayList<>();
            //pour chaque dependances definies dans le modele
            for(Model.Dependency dependency : model.getDependencies()){

                dependencies.add(statesMap.get(dependency.getDependency()));
            }

            Variable nextState = new Variable(state.getLabel(), state.getDomain(), model.getProbabilityCompute(),
                    dependencies, network.getTime() + 1);

            nextStates.add(nextState);
        }

        for (int s = 0; s < totalSamples; s++) {

            //generer un echantillon pour la coupe temporelle suivante à partir de l'echantillon courant
            //ici pour chaque variable de la requete il faut créer une variable pour l'état suivant avec ses parents
            //et leur assigner la bonne valeur en fonction de l'echantillonage courant.
            //fontionnera uniquement pour les modeles de markov d'ordre 1

            for(Variable nextState : nextStates){


            }

        }


    }

}
