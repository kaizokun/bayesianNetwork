package environment;

public class Transition {

    private double probability;

    private Action action;

    private State rsState;

    public Transition(double probability, State rsState, Action action) {
        this.probability = probability;
        this.rsState = rsState;
        this.action = action;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public State getRsState() {
        return rsState;
    }

    public void setRsState(State rsState) {
        this.rsState = rsState;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void increaseProbability(double add) {

        this.probability += add;
    }

    @Override
    public String toString() {
        return "Transition{" +
                "probability=" + probability +
                ", action=" + action +
                ", rsState=" + rsState +
                '}';
    }
}
