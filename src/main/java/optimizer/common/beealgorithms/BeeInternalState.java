package optimizer.common.beealgorithms;

import optimizer.common.InternalStateBase;
import optimizer.common.Probability;
import optimizer.objective.Objective;
import optimizer.objective.Relation;
import optimizer.trial.IterationResult;

import java.util.*;

public class BeeInternalState<AlgoPhase> extends InternalStateBase<Bee> {
    public AlgoPhase phase;

    public BeeInternalState() {
        super();
    }

    public List<Probability> createProbabilities(int numOfEmployers)
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
        //probs.add(new Probability(0,0.0));

        double max = Math.abs(swarm.get(0).actualFitness.getFitness());
        for (int i = 1; i < numOfEmployers; ++i)
        {
            if (Math.abs(swarm.get(i).actualFitness.getFitness()) > max) {
                max = Math.abs(swarm.get(i).actualFitness.getFitness());
            }

        }

        double denom = 0;
        for (int i = 0; i < numOfEmployers; ++i)
        {
            double normalizedNum = swarm.get(i).actualFitness.getFitness() / max;
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

        probs.addFirst(new Probability(-1,0.0));

        Collections.sort(probs);

        return probs;
    }

    public void createIntervals(List<Probability> probs) {
        for (int i = 0; i < probs.size() - 1; ++i) {
            probs.get(i + 1).setProbability(probs.get(i).getProbability() + probs.get(i + 1).getProbability());
        }
        probs.get(probs.size() - 1).setProbability(Math.round(probs.get(probs.size() - 1).getProbability()));
    }

    public void setBest(Bee bee) throws CloneNotSupportedException {
        if(bee.actualFitness.betterThan(swarmBestFitness)) {
            swarmBestFitness = bee.actualFitness;
            swarmBestKnownPosition = bee.position.clone();
        }
    }

    public void setAllResults(List<IterationResult> results) throws CloneNotSupportedException {
        for (IterationResult res : results) {
            Bee bee = swarm.get(res.getConfiguration().get(0).getId());
            bee.actualFitness = res;
            bee.position = bee.newPosition.clone();
            setBest(bee);
        }
    }


    /*
     * Move bee with "beeId" towards the otherBeeId
     * in a random dimension
     */
    public void moveBee(int beeId, int otherBeeId, Random rand) {
        // select random dimension
        int d = rand.nextInt(dimension);
        // create new solution
        swarm.get(beeId).newPosition = swarm.get(beeId).position.clone();
        // [-1,1] , min + Math.random() * (max - min);
        swarm.get(beeId).newPosition[d] +=
                (rand.nextFloat() * 2 - 1) *
                        (swarm.get(beeId).position[d] - swarm.get(otherBeeId).position[d]);

        swarm.get(beeId).checkBoundsForNewPosition(dimension, lowerBounds, upperBounds);
    }

    /*
     * Generate an int between 0 and max with
     * the exclusion of "excluded"
     */
    public int getRandomBee(int excluded, int max, Random rand) {
        int num = rand.nextInt(max - 1);
        if (num >= excluded) {
            ++num;
        }
        return num;
    }
}
