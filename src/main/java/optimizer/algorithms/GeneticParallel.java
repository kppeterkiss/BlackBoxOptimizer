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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import optimizer.param.Param;
import optimizer.trial.IterationResult;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by david on 2017. 08. 04..
 */

// optimizerParams[0] : populationSize size
// optimizerParams[1] : number of populations

public abstract class GeneticParallel extends AbstractAlgorithm {

    InternalState is;// = new InternalState();

    {
        this.optimizerParams = new LinkedList<>();
        this.optimizerParams.add(new Param(5,1000,1,"population_size"));
        this.optimizerParams.add(new Param(3,1000,1,"population_number"));
        this.optimizerParams.add(new Param(10.0f,10000f,0.0001f,"max_step_size"));

        this.parallelizable = ParallelExecution.GENERATION;

    }



    protected double[] fitnessList(double[] f){
        double translate=0f;
        for(int i=0;i<f.length;++i){
            double actualFitness = f[i];
            f[i]=f[i]+translate;
            translate+=actualFitness;
        }
        return f;
    }

    protected double[] normalizeFitness(double[] fitness){
        int length = fitness.length;
        double[] ret = new double[length];
        double sum = 0f;
        Double min = null;
        Double max = null;
        //find max and min and sum
        for(double f : fitness){
            if( min == null || min > f)
                min = f;
            if( max == null ||  f > max)
                max = f;
            sum+=f;
        }
        //todo wild guess give a positive fitness for all
        double diff = min - (max-min)/2;
        sum -=length*diff;
        int i = 0;
        for(double f : fitness) {
            ret[i++] = (f-diff)/sum;
        }
        return ret;
    }

    @Override
    public void updateParameters(List<Param> parameterMap, List<IterationResult> landscape) throws CloneNotSupportedException {

        //here parameters are kinda dummy

        if(is.firstStep){
            initSearchSpace(parameterMap);
            initIndividuals((int)optimizerParams.get(0).getValue(),parameterMap);
            is.firstStep = false;

        }
        else {
            List<List<Param>> p = selectPopulation(landscape);
            p = crossoverOnPopulation(p);
            p = mutatePopulation(p);
            is.population = p;
        }
        is.generationConter++;





    }

    protected abstract void select(List<IterationResult> landscape);
    protected abstract List<Param> crossover(List<IterationResult> landscape, int mother, int father) throws CloneNotSupportedException;


    @Override
    public void loadState(String internalStateBackupFileName) throws FileNotFoundException {
        if(this.config.getOptimizerStateBackupFilename()==null)
            return;
        else{
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(this.config.getOptimizerStateBackupFilename()));
            this.optimizerParams =  gson.fromJson(reader, HitAndRun.InternalState.class);

        }

    }

    @Override
    public void saveState(String internalStateBackupFileName) {
        if (this.config.getOptimizerStateBackupFilename() == null)
            return;
        else {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String s = gson.toJson(this.optimizerParams, HitAndRun.InternalState.class);
            try{
                //PrintWriter writer = new PrintWriter(getAlgorithmSimpleName()+"_params.json", "UTF-8");
                PrintWriter writer = new PrintWriter(this.config.getOptimizerStateBackupFilename(), "UTF-8");
                writer.println(s);
                //writer.println("The second line");
                writer.close();
            } catch (IOException e) {
                // do something
            }

        }

    }
    @Override
    public void updateConfigFromAlgorithmParams(List<Param> algParams) {
        //nothing to do here
    }


    class InternalState {


        List<List<Param>> population;
        int generationConter = 0;
        boolean firstStep=true;
        int populationSize = 1;

        //int bestChromosome = 0;

        //int mother;
        //int father;

        float[] upperBounds;
        float[] lowerBounds;
        int dimension;

    }
    protected List<List<Param>> selectPopulation(List<IterationResult> landscape) throws CloneNotSupportedException {return null;}

    protected List<List<Param>> crossoverOnPopulation(List<List<Param>> chromosomes) throws CloneNotSupportedException {
        int crossover_no = (int)optimizerParams.get(1).getValue();
        int[] indicesForCrossover = new int[crossover_no];
        Random rand = new Random();
        for(int i = 0;i<crossover_no;++i)
            indicesForCrossover[i]=rand.nextInt(chromosomes.size());
        List<List<Integer>> pairs= new LinkedList<>();
        for(int i = 0;i<crossover_no;++i) {
            int first = rand.nextInt(crossover_no);
            int second = first;
            while(second==first)
                second=rand.nextInt(crossover_no);
            List<Integer> pair = new LinkedList<>();
            pair.add(first);
            pair.add(second);
            pairs.add(pair);
        }
        int param_no = chromosomes.get(0).size();
        List<List<Param>> offsprings = new LinkedList<>();
        for( List<Integer> pair : pairs){
            List<Param> result = Param.cloneParamList(chromosomes.get(0));
            for(int i = 0; i <param_no; ++i) {
                boolean b = rand.nextBoolean();

                result.get(i).setInitValue(b ? chromosomes.get(pair.get(0)).get(i).getValue() :
                        chromosomes.get(pair.get(1)).get(i).getValue());
            }
            offsprings.add(result);
        }
        int j = 0;
        for(int i : indicesForCrossover)
            chromosomes.set(i,offsprings.get(j++));
        return chromosomes;

    }
    protected List<List<Param>> mutatePopulation(List<List<Param>> chromosomes) throws CloneNotSupportedException {
        Random rand = new Random();

        for (List<Param> chromosome : chromosomes) {
            double[] s = new double[chromosome.size()];
            for (int i = 0; i < s.length; ++i) {
                s[i] = (2 * rand.nextDouble()) - 1;
            }
            double c = (float) optimizerParams.get(2).getValue();

            c = HitAndRun.adjustC(s, c, chromosome);

            c *= rand.nextDouble();
            for (int i = 0; i < s.length; ++i) {
                chromosome.get(i).add(c * s[i]);
            }
        }
        return chromosomes;

    }

    //todo pso might be completely in unnnecesary
    protected void initSearchSpace(List<Param> parameterMap){
        is.dimension  = parameterMap.size();//(int)optimizerParams.get(0).getValue();
        is.lowerBounds = new float[is.dimension];
        is.upperBounds = new float[is.dimension];
        for(int i = 0; i < is.dimension; ++i) {
            is.lowerBounds[i] = ((Number)parameterMap.get(i).getLowerBound()).floatValue();
            is.upperBounds[i] = ((Number)parameterMap.get(i).getUpperBound()).floatValue();
        }
        //todo more understandable way..
        is.populationSize = (int)optimizerParams.get(0).getValue();
    }
    /**
     * initialize particles of the swarm with random position
     * @param numberOfParticles number of particles to create
     */
    //todo pso
    protected void initIndividuals(int numberOfParticles,List<Param> pattern) throws CloneNotSupportedException {



        //array to store all parameters
        is.population = new ArrayList<>();
        //initialize the particles, and add to particle array
        int particleNumber = numberOfParticles;
        for(int i = 0; i < particleNumber; ++i) {
            List<Param> p = Param.cloneParamList(pattern);
            randomFloatInit(p);
            is.population.add(p);
        }
    }


    @Override
    public List<List<Param>> getParameterMapBatch(List<Param> pattern)throws CloneNotSupportedException {return is.population;}
    @Override
    public void setResults(List<IterationResult> results) throws CloneNotSupportedException {}
    @Override
    public void updateGlobals() throws CloneNotSupportedException {}

}
