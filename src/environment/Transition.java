package environment;

public class Transition {

    private double probability;

    private State rsState;

    public Transition(double probability, State rsState) {
        this.probability = probability;
        this.rsState = rsState;
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

    public void increaseProbability(double add) {

        this.probability += add;
    }

    @Override
    public String toString() {
        return "Transition{" +
                "probability=" + probability +
                ", rsState=" + rsState +
                '}';
    }
}
