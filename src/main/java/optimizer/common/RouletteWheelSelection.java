package optimizer.common;

import optimizer.objective.Objective;
import optimizer.objective.Relation;
import optimizer.trial.IterationResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class RouletteWheelSelection {

    public List<Probability> createProbabilities(int numOfEmployers, List<? extends Solution> swarm , IterationResult swarmBestFitness)
    {
        boolean max_ = true;

        try {
            List<Objective> objectives = swarmBestFitness.getObjectiveContainerClone().getObjectiveListReference();
            Relation rel = objectives.get(0).getRelation();
            max_ = rel == Relation.MAXIMIZE || rel ==Relation.GREATER_THAN || rel ==Relation.MAXIMIZE_TO_CONVERGENCE;
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        LinkedList<Probability> probs = new LinkedList<>();

        double max = Math.abs(swarm.get(0).getActualFitness().getFitness());
        for (int i = 1; i < numOfEmployers; ++i)
        {
            if (Math.abs(swarm.get(i).getActualFitness().getFitness()) > max) {
                max = Math.abs(swarm.get(i).getActualFitness().getFitness());
            }

        }

        double denom = 0;
        for (int i = 0; i < numOfEmployers; ++i)
        {
            double normalizedNum = swarm.get(i).getActualFitness().getFitness() / max;
            denom += normalizedNum;
            probs.add(new Probability(i, normalizedNum));
        }

        for (int i = 0; i < numOfEmployers; ++i)
        {
            probs.get(i).setProbability(probs.get(i).getProbability() / denom);
        }

        Collections.sort(probs);

        if (!max_) {
            //exchange the probabilities
            int i = 0;
            while (i < probs.size() - i) {
                double tmp = probs.get(i).getProbability();
                probs.get(i).setProbability(probs.get(probs.size() - (i + 1)).getProbability());
                probs.get(probs.size() - (i + 1)).setProbability(tmp);
                ++i;
            }

        }

        Collections.sort(probs);

        probs.addFirst(new Probability(probs.get(0).getId(),0.0));

        return probs;
    }

    public void createIntervals(List<Probability> probs) {
        for (int i = 0; i < probs.size() - 1; ++i) {
            probs.get(i + 1).setProbability(probs.get(i).getProbability() + probs.get(i + 1).getProbability());
        }
        probs.get(probs.size() - 1).setProbability(Math.round(probs.get(probs.size() - 1).getProbability()));
    }

}
