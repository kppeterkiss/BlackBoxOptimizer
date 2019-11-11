package optimizer.common;

public class Probability  implements  Comparable<Probability>{
    public int getId() {
        return id;
    }

    int id;

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    double probability;

    public Probability(int id, double probability) {
        this.id = id;
        this.probability = probability;
    }

    @Override
    public int compareTo(Probability probability) {
        if (this.probability < probability.probability) return -1;
        if (this.probability > probability.probability) return  1;
        else return 0;
    }

    @Override
    public String toString() {
        return "id: "+ id +", probability: " + probability;
    }
}
