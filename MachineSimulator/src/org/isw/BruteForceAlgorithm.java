package org.isw;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.threads.SimulationThread;

public class BruteForceAlgorithm {
	Schedule schedule;
	int[] pmOpportunity;
	SimulationResult noPM;
	public BruteForceAlgorithm(Schedule schedule, int[] pmOpportunity, SimulationResult noPM){
		this.schedule = schedule;
		this.pmOpportunity = pmOpportunity;
		this.noPM = noPM;
		
	}
	
	public SimulationResult[] execute() throws InterruptedException, ExecutionException{
		ExecutorService threadPool = Executors.newFixedThreadPool(5);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		long cnt = 0 ;
		double max = Math.pow(2, Machine.compList.length*pmOpportunity.length);
		System.out.println("Number of solutions: "+max);
		for(long i=1 ; i < max;i++){
			pool.submit(new SimulationThread(schedule, getCombolist(i),pmOpportunity,false,i));
			cnt++;
			if(i%500000==0)
				System.out.format("Adding to thread pool %f percent\n",i/max*100);
		}
		System.out.println("Added all tasks to thread pool");
		ArrayList<SimulationResult> results = new ArrayList<SimulationResult>();
		for(long i=0;i<cnt;i++){
			SimulationResult result = pool.take().get();
			if(noPM.cost > result.cost){
				results.add(result);
			}
			if(i%10000==0)
				System.out.format("Percent complete: %f\n ",i/max*100);
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		SimulationResult[] r = new SimulationResult[results.size()];
		r = results.toArray(r);
		SimulationResult best = Collections.min(results);
		System.out.format("BF Cost: %f (%s)\n",best.cost,Long.toBinaryString(best.chromosomeID));
		
		// write results to file
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("BFCostComp.csv", true)))) {
		    out.format("%f,%s\n",best.cost,Long.toBinaryString(best.chromosomeID));
		}catch (IOException e) {
		    //exception handling left as an exercise for the reader
		}
		
		return r;
		
	}
	private long[] getCombolist(long combo) {
		long combos[] = new long[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = (combo>>(Machine.compList.length*i))&((int)Math.pow(2,Machine.compList.length)-1);
		}
		return combos;
	}
}
