/*
 *   Copyright 2018 Peter Kiss and David Fonyo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package optimizer.algorithms;

import optimizer.trial.IterationResult;
import optimizer.param.Param;
import optimizer.utils.Utils;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 12..
 */
public class ParticleSwarmOptimization extends AbstractAlgorithm {
    InternalState is = new InternalState();


    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(5,Integer.MAX_VALUE,Integer.MIN_VALUE,"swarm_size"));
        this.optimizerParams.add(new Param(1.0, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "omega"));//w weigth of previous velocity
        this.optimizerParams.add(new Param(1.0, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "phi_p"));//c1 weight of direction to personal best
        this.optimizerParams.add(new Param(1.0, Utils.FLOAT_REDEFINED_MAX_VALUE,Float.MIN_VALUE, "phi_g"));//c2 weight of direction to personal best
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {
            /**
             * number of particles in the swarm
             */
            int particleNumber=(int)optimizerParams.get(0).getValue();
            if(is.initialisedParticles == 0) {
                //how many parameters we have to optmize
                initParticles(parameterMap);
            }
            //stage 1 initialization of all the particles - that is assign the first fitness and send the next for evaluation
            //set the value of the parameter to the one in the particle for running the experiment
            if(is.initialisedParticles < particleNumber) {
                // send the last initialized particle for evaluation
                setParticlePositionForParameterSetup(parameterMap,is.swarm.get(is.initialisedParticles));
                // read the fitness from the previous round and set for previous particle, except if this is the very first round
                if(is.initialisedParticles != 0)
                    //is.swarm.get(is.initialisedParticles-1).bestFitness = (float) landscape.get(landscape.size()-1).getFitness();
                    is.swarm.get(is.initialisedParticles-1).bestFitness =  landscape.get(landscape.size()-1);
                ++is.initialisedParticles;
                return;
            }
            //stage 2. if all the results are done particles are initialized - all the particles have a fitness now
            if(is.initialisedParticles == particleNumber) {
                //set the lasts best fitness to the fitnes came back inn the last place of the landscape
                //only for the last round
                is.swarm.get(is.initialisedParticles-1).bestFitness =  landscape.get(landscape.size()-1);
                ++is.initialisedParticles;

                //update best fitness value and best
                updateGlobalBestPositionAndFitness();
            }

        //after we have all the parameters evaluated
        Particle particle = is.swarm.get(is.actualParticle);
        if(!is.firstStep) {
                // if recent fitness of th particle is worse than the best it reached
           IterationResult ir = landscape.get(landscape.size()-1);

            updateParticleAndGlobalState(particle, ir);
            is.firstStep = false;

                ++is.actualParticle;
                if(is.actualParticle == particleNumber)
                    is.actualParticle = 0;
            }
        updateParticleVelocity(parameterMap.size(), particle);
        for(int d = 0; d < parameterMap.size(); ++d)
            parameterMap.get(d).setInitValue(particle.position[d]);



    }

    protected void updateParticleAndGlobalState(Particle particle, IterationResult ir) throws CloneNotSupportedException {
        if(ir.betterThan(particle.bestFitness) ){

            particle.bestKnownPosition = particle.position.clone();
            particle.bestFitness = ir;

            if(particle.bestFitness.betterThan(is.swarmBestFitness)) {
                is.swarmBestFitness = particle.bestFitness;
                is.swarmBestKnownPosition = particle.bestKnownPosition.clone();
            }
        }
    }

    protected void updateParticleVelocity(int dimension, Particle particle) {
        // update the velocity TODO new random for each coordinates??? and this is done not at the end of the cycle... data might be outdated
        for(int d = 0; d < dimension; ++d) {


            Random rand = new Random();
            float randomFactorForParticleBest = rand.nextFloat();
            float randomFactorForGlobalBest = rand.nextFloat();

            particle.velocity[d] = particle.velocity[d]
                    * /*(double)*/((Number)optimizerParams.get(1).getValue()).floatValue() +
                    /*(double)*/((Number)optimizerParams.get(2).getValue()).floatValue() * randomFactorForParticleBest * (particle.bestKnownPosition[d] - particle.position[d]) +
                    /*(double)*/((Number)optimizerParams.get(3).getValue()).floatValue() * randomFactorForGlobalBest * (is.swarmBestKnownPosition[d] - particle.position[d]);


            particle.position[d] += particle.velocity[d];
        }
    }

    private void updateGlobalBestPositionAndFitness() throws CloneNotSupportedException {
        is.swarmBestKnownPosition = is.swarm.get(0).position.clone();
        is.swarmBestFitness = is.swarm.get(0).bestFitness;

        for(int i = 1; i < is.swarm.size(); ++i) {

            if(is.swarmBestFitness == null || is.swarm.get(i).bestFitness.betterThan(is.swarmBestFitness)) {
                is.swarmBestFitness = is.swarm.get(i).bestFitness;
                is.swarmBestKnownPosition = is.swarm.get(i).bestKnownPosition.clone();
            }
        }
    }

    protected void setParticlePositionForParameterSetup(List<Param> parameterMap, Particle particle) {
        for(int i = 0; i < parameterMap.size(); ++i) {
            parameterMap.get(i).setInitValue(particle.position[i]);
        }
    }

    protected void initParticles(List<Param> parameterMap) {
        int dimension = parameterMap.size();
        float[] lower_bound = new float[dimension];
        float[] upper_bound = new float[dimension];
        for(int i = 0; i < dimension; ++i) {
            lower_bound[i] = ((Number)parameterMap.get(i).getLowerBound()).floatValue();
            upper_bound[i] = ((Number)parameterMap.get(i).getUpperBound()).floatValue();
        }
        //array to store all parameters
        is.swarm = new ArrayList<>();
        //initialize the particles, and add to particle array
        int particleNumber = ((Number) optimizerParams.get(0).getValue()).intValue();
        for(int i = 0; i < particleNumber; ++i) {
            is.swarm.add(new Particle(dimension,lower_bound,upper_bound));
        }
    }


    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
    }

    @Override
    public void saveState(String internalStateBackupFileName) {

    }
    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //nothing to do here
    }
    //todo Do we need this?
    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern) throws CloneNotSupportedException {return null;}
    @Override
    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {}

    class Particle {
        /**
         * actual positions
         */
        float[] position;
        /**
         *velocities of the individuals respectively
         */
        float[] velocity;
        /**
         * best positions for the individuals respectively
         */
        float[] bestKnownPosition;
        /**
         * global best fitness
         */
        //float bestFitness;
        IterationResult bestFitness;

        public Particle(int dim, float[] lowerBounds, float[] upperBounds) {
            /**
             * recent position
             */
            position = new float[dim];
            /**
             * velocity vector to describe movement of the particle
             */
            velocity = new float[dim];
            /**
             * best position of the particle up to now
             */
            bestKnownPosition = new float[dim];

            Random rand = new Random();
            for(int i = 0; i < dim; ++i) {
                float r = rand.nextFloat();
                //random initialization of position
                position[i] = lowerBounds[i] + r * (upperBounds[i] - lowerBounds[i]);
                bestKnownPosition[i] = position[i];
                r = rand.nextFloat();
                //random initialization of velocity
                velocity[i] = lowerBounds[i] - upperBounds[i] + 2 * r * (upperBounds[i] - lowerBounds[i]);
            }
            //bestFitness = 0f;
            bestFitness = null;

        }
    }

    class InternalState {
        ArrayList<Particle> swarm;

        /**
         * pointer to the particle to be evaluated
         */
        int actualParticle;
        /**
         * used as a pointer to the actual particle
         */
        int initialisedParticles = 0;
        /**
         * position of the best result
         */
        float[] swarmBestKnownPosition;
        /**
         * best result of the swarn
         */
        IterationResult swarmBestFitness;
        boolean firstStep = true;

    }
}
