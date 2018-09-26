package network.dynamic;

import domain.data.AbstractDoubleFactory;
import inference.dynamic.Forward;
import network.Variable;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class PDMPO extends DynamicBayesianNetwork{


    protected Map<Variable, List<Model>> actionsModels = new Hashtable<>();

    public PDMPO(AbstractDoubleFactory doubleFactory, Forward forward, int time) {
        super(doubleFactory, forward, time);
    }

    public PDMPO(AbstractDoubleFactory doubleFactory, int time) {
        super(doubleFactory, time);
    }

    public PDMPO(AbstractDoubleFactory doubleFactory) {
        super(doubleFactory);
    }


    public void addActionModel(Variable variable, Model model) {
        //recupere la liste des modeles par limite de temps atteinte pour une variable
        addModel(variable, model, actionsModels);
    }
}
