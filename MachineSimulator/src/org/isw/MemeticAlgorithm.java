package org.isw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.isw.threads.SimulationThread;

public class MemeticAlgorithm {
	private int populationSize;
	private int stopCrit;
	private int pmOpportunity[];
	private Schedule schedule;
	private ArrayList<Chromosome> population;
	private ArrayList<Chromosome> offsprings;
	private Chromosome bestChromosome;
	private double totalFitness;
	Random rand;
	EnumeratedDistribution<Chromosome> distribution;
	public MemeticAlgorithm(int populationSize, int stopCrit, Schedule schedule ,int[] pmOpportunity){
		this.populationSize = populationSize;
		this.population = new ArrayList<Chromosome>();
		this.stopCrit = stopCrit;
		this.schedule = schedule;
		this.pmOpportunity = pmOpportunity;
		bestChromosome = new Chromosome(0);
		rand = new Random();
	}
	
	public SimulationResult execute() throws InterruptedException, ExecutionException{
		initializePopulation();
		int cnt=0;
		while(true){
			optimizePopulation();
			evaluateFitness(population);
			distribution = new EnumeratedDistribution<Chromosome>(populationDistribution());
			if(cnt++ >= stopCrit)
				break;
			generatePopulation();
		}
		return null;
	}

	private void generatePopulation() throws InterruptedException, ExecutionException {
	//TODO: Mutation, crossover ratio
		offsprings = new ArrayList<Chromosome>();
		int numberOfPairs = (populationSize/4%2==0)?populationSize/4:populationSize/4+1; 
		for(int i=0;i<numberOfPairs;i++){
			Chromosome[] parents = selectParents();
			if(parents[0].combo != parents[1].combo){
					Chromosome[] offspring = crossover(parents[0],parents[1]);
					offsprings.add(offspring[0]);	
					offsprings.add(offspring[1]);	
					}
		}
		//Do mutation here
		evaluateFitness(offsprings);
		population.addAll(offsprings);
		Collections.sort(population,Collections.reverseOrder());
		population = (ArrayList<Chromosome>) population.subList(0, populationSize);
	}
	
	//Single point crossover
	private Chromosome[] crossover(Chromosome parent1, Chromosome parent2) {
		Chromosome[] offsprings = new Chromosome[2];
		int combo1[] = {parent1.combo,parent1.combo};
		int combo2[] = {parent2.combo,parent2.combo};
		
		int crossoverPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length-1) + 1;
		for(int i=0; i<Machine.compList.length*pmOpportunity.length;i++){
			if(i < crossoverPoint){
				combo2[0] = combo2[0] & ~(1<<i);
				combo1[1] = combo2[1] & ~(1<<i);
			}
			else{
				combo1[0] = combo1[0] & ~(1<<i);
				combo2[1] = combo2[1] & ~(1<<i);
			}
		}
		offsprings[0] = new Chromosome(combo1[0]|combo2[0]);
		offsprings[1] = new Chromosome(combo1[1]|combo2[1]);
		return offsprings;
	}


	private Chromosome[] selectParents() {
		return (Chromosome[]) distribution.sample(2);
	}
	
	//Distribution for the roulette wheel selection
	private List<Pair<Chromosome, Double>> populationDistribution() {
		ArrayList<Pair<Chromosome, Double>> dist = new ArrayList<Pair<Chromosome, Double>>();
		for(int i=0;i<populationSize;i++){
			dist.add(new Pair<Chromosome, Double>(population.get(i),population.get(i).fitnessValue/totalFitness));
		}
		return dist;
	}

	private void evaluateFitness(ArrayList<Chromosome> list) throws InterruptedException, ExecutionException {
		HashMap<Integer, Chromosome> map = new HashMap<Integer, Chromosome>(); 
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		int cnt = 0 ;
		for(Chromosome chromosome : list){
			map.put(chromosome.combo, chromosome);
			pool.submit(new SimulationThread(schedule,chromosome.getCombolist(),pmOpportunity,false));
			cnt++;
		}
		for(int i=0;i<cnt;i++){
			SimulationResult result = pool.take().get();
			map.get(result.getChormosome()).fitnessValue = result.cost;
			
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		 
	}

	private void optimizePopulation() {
		for(Chromosome chromosome: population){
			chromosome.applyHeuristic();
		}	
	}

	private void initializePopulation() {
		
		for(int i=0;i<populationSize;i++){
			  population.add(new Chromosome(rand.nextInt((int)Math.pow(2, Machine.compList.length*pmOpportunity.length)) + 1));
		}
		
	}
	
}
class Chromosome implements Comparable<Chromosome>{
	
	double fitnessValue;
	//Binary representation of the chromosome
	int combo;
	public Chromosome(int combo){
		this.combo = combo;
		this.fitnessValue = 0;
	}
	
	public int[] getCombolist() {
		
		return null;
	}

	public void applyHeuristic() {
	//TODO	
		
	}

	@Override
	public int compareTo(Chromosome o) {
		return Double.compare(fitnessValue, o.fitnessValue);
	}

}
