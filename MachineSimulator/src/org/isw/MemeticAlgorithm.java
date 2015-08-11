package org.isw;

import java.util.ArrayList;
import java.util.Collections;
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
	int pmOpportunity[];
	Schedule schedule;
	private ArrayList<Chromosome> population;
	private ArrayList<Chromosome> offsprings;
	SimulationResult noPM;
	private double totalFitness;
	Random rand;
	EnumeratedDistribution<Chromosome> distribution;
	public MemeticAlgorithm(int populationSize, int stopCrit, Schedule schedule ,int[] pmOpportunity, SimulationResult noPM){
		this.populationSize = populationSize;
		this.population = new ArrayList<Chromosome>();
		this.stopCrit = stopCrit;
		this.schedule = schedule;
		this.pmOpportunity = pmOpportunity;
		rand = new Random();
		this.noPM = noPM;
	}
	
	public SimulationResult[] execute() throws InterruptedException, ExecutionException{
		initializePopulation();
		int cnt=0;
		while(true){
			totalFitness = 0;
			evaluateFitness(population);
			try{
			distribution = new EnumeratedDistribution<Chromosome>(populationDistribution());
			}catch(Exception e){
				e.printStackTrace();
				
			}
			//System.out.format("%f (%s)\n",population.get(0).fitnessValue,Integer.toBinaryString(population.get(0).combo));
			if(cnt++ >= stopCrit)
				break;
			
			generatePopulation();
		}
		
		Collections.sort(population);
		System.out.format("%f (%s)\n",population.get(0).fitnessValue,Integer.toBinaryString(population.get(0).combo));
		int i =0;
		ArrayList<SimulationResult> results = new ArrayList<SimulationResult>();
		System.out.println("----------------------------------------");
		while(i < populationSize && population.get(i).fitnessValue < noPM.cost){
			Chromosome c = population.get(i);
			if(c.combo != 0){
				//System.out.format("%f (%s)\n",c.fitnessValue,Integer.toBinaryString(c.combo));
				results.add(new SimulationResult(c.fitnessValue,c.pmAvgTime,c.getCombolist(),pmOpportunity,false,i));
				
			}
			i++;
		}
		SimulationResult[] r = new SimulationResult[results.size()];
		r = results.toArray(r);
		return r;
	}

	private void generatePopulation() throws InterruptedException, ExecutionException {
	//TODO: Mutation, crossover ratio
		offsprings = new ArrayList<Chromosome>();
		int numberOfPairs = (populationSize/4%2==0)?populationSize/4:populationSize/4+1; 
		for(int i=0;i<numberOfPairs;i++){
			Chromosome[] parents = selectParents();
			if(parents[0].combo != parents[1].combo){
					Chromosome[] offspring = crossover(parents[0],parents[1]);
					if(offspring[0].combo != 0)
						offsprings.add(offspring[0]);
					if(offspring[0].combo != 0)
						offsprings.add(offspring[1]);	
					}
		}
		//Do mutation here
		
		for(Chromosome offspring: offsprings){
			if(rand.nextDouble() < 0.4){
				int mutationPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length);
				if((offspring.combo^1<<mutationPoint) !=0)
					offspring.combo ^= 1<<mutationPoint;
				
				mutationPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length);
				if((offspring.combo^1<<mutationPoint) !=0)
					offspring.combo ^= 1<<mutationPoint;
			}
		}
		optimizeOffsprings();
		evaluateFitness(offsprings);
		population.addAll(offsprings);
		Collections.sort(population);
		population.subList(populationSize-1, population.size()-1).clear();
	}
	
	//Single point crossover
	private Chromosome[] crossover(Chromosome parent1, Chromosome parent2) {
		Chromosome[] offsprings = new Chromosome[2];
		int combo1[] = {parent1.combo,parent1.combo};
		int combo2[] = {parent2.combo,parent2.combo};
		
		int crossoverPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length-1)+1;
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
		
		offsprings[0] = new Chromosome(combo1[0]|combo2[0],this);
		offsprings[1] = new Chromosome(combo1[1]|combo2[1],this);
		return offsprings;
	}


	private Chromosome[] selectParents() {
		Chromosome parents[] = new Chromosome[2];
		distribution.sample(2,parents);
		return parents;
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
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		int cnt = 0 ;
		for(Chromosome chromosome : list){
			pool.submit(new SimulationThread(schedule,chromosome.getCombolist(),pmOpportunity,false,cnt));
			cnt++;
		}
		for(int i=0;i<cnt;i++){
			SimulationResult result = pool.take().get();
			list.get(result.chromosomeID).fitnessValue = result.cost;
			list.get(result.chromosomeID).pmAvgTime = result.pmAvgTime;
			totalFitness += result.cost;
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		 
	}

	private void optimizeOffsprings() {
		for(Chromosome chromosome: offsprings){
			chromosome.applyLocalSearch();
		}	
	}

	private void initializePopulation() {
		ArrayList<Integer> numbers = new ArrayList<Integer>();
		for(int i=1 ; i < Math.pow(2, Machine.compList.length*pmOpportunity.length);i++){
			numbers.add(i);
		}
		Collections.shuffle(numbers);
		for(int i=0;i<populationSize;i++){
			  population.add(new Chromosome(numbers.get(i),this));
		}
		
	}
	
}
class Chromosome implements Comparable<Chromosome>{
	
	double pmAvgTime;
	double fitnessValue;
	int[] pmOpportunity;
	Schedule schedule;
	//Binary representation of the chromosome
	int combo;
	public Chromosome(int combo ,MemeticAlgorithm ma){
		this.combo = combo;
		this.fitnessValue = 0;
		this.pmOpportunity = ma.pmOpportunity;
		this.schedule = ma.schedule;
	}
	
	public int[] getCombolist() {
		int combos[] = new int[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = (combo>>(Machine.compList.length*i))&((int)Math.pow(2,Machine.compList.length)-1);
		}
		return combos;
	}

	public void applyLocalSearch() {
		int maxCombo = combo;
		if(heuristic(maxCombo) < heuristic(combo+1) && combo + 1 != Math.pow(2, Machine.compList.length*pmOpportunity.length))
				maxCombo = combo+1;
		if(heuristic(maxCombo) < heuristic(combo-1) && combo != 1)
			maxCombo = combo-1;
		combo = maxCombo;
	}
	


	private int heuristic(int combo) {
		Component[] temp = Machine.compList.clone();
		int heuristicP=0;
		int heuristicN=0;
		Double fp;
		int comboList[] = getCombolist(combo);
		for(int i=0;i<comboList.length;i++){
			for(int j=0;j<temp.length;j++){
				int pos = 1<<j;
				if((comboList[i]&pos)!=0){
					fp = getFailureProbablity(temp[j]);
					heuristicP += (fp>0.5d)?1:-1;
					temp[i].initAge = (1-temp[i].pmRF)*temp[i].initAge;
				}
				else{
					fp = getFailureProbablity(temp[j]);
					heuristicN += (fp<0.5d)?1:-1;
				}
			}
		}
		return heuristicN+heuristicP;
	}

	private Double getFailureProbablity(Component component , int oppoIndex) {
		int failureCount =0;
		long time = 0;
		if(oppoIndex ==  pmOpportunity.length-1)
			time = Math.min(Macros.SHIFT_DURATION, schedule.getSum());
		else 
			time = schedule.getFinishingTime(pmOpportunity[oppoIndex+1]-1);
		for(int i=0;i<1000;i++){
			Double cmTTF = component.getCMTTF();
			if(cmTTF < time){
				failureCount++;
			}
		}
		return failureCount/1000d;
	}
	
	private Double getFailureProbablity(Component component) {
		int failureCount =0;
		long time = 0;
		time = Math.min(Macros.SHIFT_DURATION, schedule.getSum());
	
		for(int i=0;i<1000;i++){
			Double cmTTF = component.getCMTTF();
			if(cmTTF < time){
				failureCount++;
			}
		}
		return failureCount/1000d;
	}

	private int[] getCombolist(int combo) {
		int combos[] = new int[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = (combo>>(Machine.compList.length*i))&((int)Math.pow(2,Machine.compList.length)-1);
		}
		return combos;
	}

	@Override
	public int compareTo(Chromosome o) {
		return Double.compare(fitnessValue, o.fitnessValue);
	}

}
