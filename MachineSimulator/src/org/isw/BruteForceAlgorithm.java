package org.isw;

import java.util.ArrayList;
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
		for(long i=1 ; i < max;i++){
			pool.submit(new SimulationThread(schedule, getCombolist(i),pmOpportunity,false,i));
			cnt++;
		}
		ArrayList<SimulationResult> results = new ArrayList<SimulationResult>();
		for(long i=0;i<cnt;i++){
			SimulationResult result = pool.take().get();
			if(noPM.cost > result.cost){
				result.cost = noPM.cost - result.cost;
				results.add(result);
			}
			if(i == (long)cnt/4)
				System.out.println("25%");
			if(i == (long)cnt/2)
				System.out.println("50%");
			if(i == (long)3*cnt/4)
				System.out.println("75%");

		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());

		SimulationResult[] results2 = new SimulationResult[results.size()];
		for(int i=0;i<results.size();i++){
			results2[i] = results.get(i);
		}
		return results2;
	}
	private long[] getCombolist(long combo) {
		long combos[] = new long[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = (combo>>(Machine.compList.length*i))&((int)Math.pow(2,Machine.compList.length)-1);
		}
		return combos;
	}
}
