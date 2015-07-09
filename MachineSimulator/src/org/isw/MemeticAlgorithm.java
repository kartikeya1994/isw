package org.isw;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;

public class MemeticAlgorithm {
	private int populationSize;
	private int stopCrit;
	private int pmOpportunity;
	private Schedule schedule;
	private Chromosome[] population;
	private Chromosome bestChromosome;
	private double totalFitness;
	public MemeticAlgorithm(int populationSize, int stopCrit, Schedule schedule ,int pmOpportunity){
		this.populationSize = populationSize;
		this.population = new Chromosome[populationSize];
		this.stopCrit = stopCrit;
		this.schedule = schedule;
		this.pmOpportunity = pmOpportunity;
		bestChromosome = new Chromosome(0);
	}
	
	public SimulationResult execute(){
		initializePopulation();
		int cnt=0;
		while(true){
			optimizePopulation();
			evaluateFitness();
			if(cnt++ >= stopCrit)
				break;
			Chromosome[] parents = selectParents();
			generatePopulation(parents);
		}
		return null;
	}

	private void generatePopulation(Chromosome[] parents) {
		//TODO
		
	}

	private Chromosome[] selectParents() {
		EnumeratedDistribution<Chromosome> distribution = new EnumeratedDistribution<Chromosome>(populationDistribution());
		return (Chromosome[]) distribution.sample((populationSize/2%2==0)?populationSize/2:populationSize/2+1);
	}

	private List<Pair<Chromosome, Double>> populationDistribution() {
		ArrayList<Pair<Chromosome, Double>> dist = new ArrayList<Pair<Chromosome, Double>>();
		for(int i=0;i<populationSize;i++){
			dist.add(new Pair<Chromosome, Double>(population[i],population[i].fitnessValue/totalFitness));
		}
		return dist;
	}

	private void evaluateFitness() {
		//TODO
		
	}

	private void optimizePopulation() {
		for(Chromosome chromosome: population){
			chromosome.applyHeuristic();
		}	
	}

	private void initializePopulation() {
		Random rand = new Random();
		for(int i=0;i<populationSize;i++){
			  population[i] = new Chromosome(rand.nextInt((int)Math.pow(2, Machine.compList.length)) + 1);
		}
		
	}
	
}
class Chromosome{
	double fitnessValue;
	int combo;
	public Chromosome(int combo){
		this.combo = combo;
		this.fitnessValue = 0;
	}
	
	public void applyHeuristic() {
	//TODO	
		
	}

}
