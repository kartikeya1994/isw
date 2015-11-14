package org.isw;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
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
	Chromosome best;
	Schedule schedule;
	private ArrayList<Chromosome> population;
	private ArrayList<Chromosome> offsprings;
	SimulationResult noPM;
	private double totalFitness;
	Random rand;
	EnumeratedDistribution<Chromosome> distribution;
	private int convergenceCount;
	private boolean noLS;
	public MemeticAlgorithm(int populationSize, int stopCrit, Schedule schedule ,int[] pmOpportunity, SimulationResult noPM, boolean noLS){
		this.populationSize = populationSize;
		this.population = new ArrayList<Chromosome>();
		this.stopCrit = stopCrit;
		this.schedule = schedule;
		this.pmOpportunity = pmOpportunity;
		rand = new Random();
		this.noPM = noPM;
		best = new Chromosome(new BigInteger("0") ,this);
		best.fitnessValue = 0;
		convergenceCount = 0;
		this.noLS = noLS;
	}

	public SimulationResult[] execute() throws InterruptedException, ExecutionException, NumberFormatException, IOException
	{
		System.out.println("Initializing population");
		initializePopulation();
		System.out.println("Evaluating fitness");
		evaluateFitness(population);
		int cnt=0;
		while(true)
		{
			totalFitness = 0;
			for(Chromosome individual: population)
				totalFitness += individual.fitnessValue;

			try
			{
				distribution = new EnumeratedDistribution<Chromosome>(populationDistribution());
			}catch(Exception e)
			{
				e.printStackTrace();
			}
			
			if(cnt++ >= stopCrit || hasConverged())
				break;
			
			System.out.println("Generation: "+cnt);
			generatePopulation();
			
			if(cnt%20==0)
				System.out.format("%d%%",(int)(cnt/(float)stopCrit*100));
			//System.out.format("%d,%f\n",cnt,population.get(0).fitnessValue);
		}

		Collections.sort(population);
		//System.out.format("%f (%s)\n",population.get(0).fitnessValue,Long.toBinaryString(population.get(0).combo));


		int i =0;
		ArrayList<SimulationResult> results = new ArrayList<SimulationResult>();
		HashMap<BigInteger,Boolean> hm = new HashMap<BigInteger, Boolean>();
		while(i < populationSize && population.get(i).fitnessValue < noPM.cost){
			Chromosome c = population.get(i);
			if((!c.combo.equals(BigInteger.valueOf(0))) && !hm.containsKey(c.combo)){
				hm.put(c.combo, true);
				//System.out.format("%f (%s) ",c.fitnessValue,Integer.toBinaryString(c.combo));
				results.add(new SimulationResult(noPM.cost - c.fitnessValue, c.pmAvgTime,c.getCombolist(),pmOpportunity,false,i));

			}
			i++;
		}
		SimulationResult[] r = new SimulationResult[results.size()];
		r = results.toArray(r);
		return r;
	}



	private boolean hasConverged() {
		if(population.get(0).combo == best.combo){
			convergenceCount++;
			if(convergenceCount == 25)
				return true;
		}
		else{
			convergenceCount = 0;
			best.combo =  population.get(0).combo;
		}
		return false;
	}

	private void generatePopulation() throws InterruptedException, ExecutionException {
		//TODO: Mutation, crossover ratio
		offsprings = new ArrayList<Chromosome>();
		int numberOfPairs = (populationSize/4%2==0)?populationSize/4:populationSize/4+1; 
		for(int i=0;i<numberOfPairs;i++)
		{
			Chromosome[] parents = selectParents();
			if(!parents[0].combo.equals(parents[1].combo)){
				Chromosome[] offspring = crossover(parents[0],parents[1]);
				if(!offspring[0].combo.equals(BigInteger.valueOf(0)))
					offsprings.add(offspring[0]);
				if(!offspring[0].combo.equals(BigInteger.valueOf(0)))
					offsprings.add(offspring[1]);	
			}
		}
		//Do mutation here
		for(Chromosome offspring: offsprings){
			if(rand.nextDouble() < 0.4){
				int mutationPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length-1);

				do
				{
					offspring.combo = offspring.combo.flipBit(mutationPoint);
					mutationPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length-1);
				}while(offspring.combo.equals(BigInteger.valueOf(0)));
			}
		}

		if(!noLS){
			optimizeOffsprings();
		}
		evaluateFitness(offsprings);
		population.addAll(offsprings);
		Collections.sort(population);
		population.subList(populationSize-1, population.size()-1).clear();
	}

	//Single point crossover
	private Chromosome[] crossover(Chromosome parent1, Chromosome parent2) {
		Chromosome[] offsprings = new Chromosome[2];
		BigInteger rights[] = {parent1.combo,parent1.combo};
		BigInteger lefts[] = {parent2.combo,parent2.combo};

		int crossoverPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length-2)+1;

		rights[0] = rights[0].and(BigInteger.valueOf(1<<crossoverPoint-1));
		rights[1] = rights[1].and(BigInteger.valueOf(1<<crossoverPoint-1));

		lefts[0] = lefts[0].shiftRight(crossoverPoint).shiftLeft(crossoverPoint);
		lefts[1] = lefts[1].shiftRight(crossoverPoint).shiftLeft(crossoverPoint);	

		offsprings[0] = new Chromosome(lefts[0].or(rights[1]),this);
		offsprings[1] = new Chromosome(lefts[1].or(rights[0]),this);

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
		ExecutorService threadPool = Executors.newFixedThreadPool(5);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		int cnt = 0 ;

		for(Chromosome chromosome : list){
			pool.submit(new SimulationThread(schedule,chromosome.getCombolist(),pmOpportunity,false,cnt));
			cnt++;
		}
		for(int i=0;i<cnt;i++){
			SimulationResult result = pool.take().get();
			list.get((int)result.chromosomeID).fitnessValue = result.cost;
			list.get((int)result.chromosomeID).pmAvgTime = result.pmAvgTime;
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



	private void initializePopulation() throws NumberFormatException, IOException 
	{
		//BufferedReader br = new BufferedReader(new FileReader("init_population"));

		/*
		 * Initialize population of size 2*number of components * number of pm Opp
		 */
		int bits = Machine.compList.length*pmOpportunity.length;
		BigInteger num;
		Random rnd = new Random();
		Hashtable<BigInteger, Boolean> hashTable = new Hashtable<BigInteger, Boolean>();
		for(int i=0;i<populationSize;i++)
		{

			//num = Long.parseLong(br.readLine());
			num = new BigInteger(bits, rnd);
			while(hashTable.containsKey(num))
			{
				num = new BigInteger(bits, rnd);
			}

			population.add(new Chromosome(num,this));
			hashTable.put(num, new Boolean(true));
		}
	}
}
class Chromosome implements Comparable<Chromosome>{
	static Random rand = new Random();
	double pmAvgTime;
	double fitnessValue;
	int[] pmOpportunity;
	Schedule schedule;
	//Binary representation of the chromosome
	BigInteger combo;
	public Chromosome(BigInteger combo ,MemeticAlgorithm ma){
		this.combo = combo;
		this.fitnessValue = 0;
		this.pmOpportunity = ma.pmOpportunity;
		this.schedule = ma.schedule;
	}

	public long[] getCombolist() {
		long combos[] = new long[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = combo.shiftRight(Machine.compList.length*i).and(BigInteger.valueOf((long)Math.pow(2,Machine.compList.length)-1)).longValue();
		}
		return combos;
	}

	public void applyLocalSearch() 
	{
		BigInteger new_combo = combo;
		int cnt = 0;
		while(cnt++<5)
		{
			int mutationPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length-1);
			do
			{
				new_combo = combo.flipBit(mutationPoint);
				mutationPoint = rand.nextInt(Machine.compList.length*pmOpportunity.length-1);
			}while(new_combo.equals(BigInteger.valueOf(0)));

			if(heuristic(new_combo)>heuristic(combo)){
				combo = new_combo;
			}
		}		
	}



	private long heuristic(BigInteger combo) {
		Component[] temp = new Component[Machine.compList.length];
		for(int i=0; i < Machine.compList.length; i++)
			temp[i] = new Component(Machine.compList[i]);

		int heuristicP=0;
		int heuristicN=0;
		Double fp;
		long comboList[] = getCombolist(combo);
		for(int i=0;i<comboList.length;i++){
			for(int j=0;j<temp.length;j++){
				int pos = 1<<j;
				if((comboList[i]&pos)!=0){
					fp = getFailureProbablity(temp[j], pmOpportunity[i]);
					heuristicP += (fp>0.5d)?1:-1;
					temp[j].initAge = (1-temp[j].pmRF)*temp[j].initAge;
				}
				else{
					fp = getFailureProbablity(temp[j], pmOpportunity[i]);
					heuristicN += (fp<0.5d)?1:-1;
				}
			}
		}
		return heuristicN+heuristicP;
	}




	private Double getFailureProbablity(Component component, int pmO) {
		int failureCount =0;
		long time = 0;
		time = Math.min(Macros.SHIFT_DURATION, schedule.getSum());
		time -= pmO;
		for(int i=0;i<50;i++){
			Double cmTTF = component.getCMTTF();
			if(cmTTF < time){
				failureCount++;
			}
		}
		return failureCount/50d;
	}

	private long[] getCombolist(BigInteger combo) {
		long combos[] = new long[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = combo.shiftRight(Machine.compList.length*i).and( BigInteger.valueOf((long) Math.pow(2,Machine.compList.length)-1) ).longValue();
		}
		return combos;
	}

	@Override
	public int compareTo(Chromosome o) {
		return Double.compare(fitnessValue, o.fitnessValue);
	}

}
