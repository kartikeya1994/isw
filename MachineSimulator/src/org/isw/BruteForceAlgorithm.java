package org.isw;

import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.threads.SimulationThread;

public class BruteForceAlgorithm {
	Schedule schedule;
	int[] pmOpportunity;
	SimulationResult best;  
	public BruteForceAlgorithm(Schedule schedule, int[] pmOpportunity, SimulationResult noPM){
		this.schedule = schedule;
		this.pmOpportunity = pmOpportunity;
		this.best = noPM;
		
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
		for(long i=0;i<cnt;i++){
			SimulationResult result = pool.take().get();
			if(best.cost > result.cost){
				best.cost = result.cost;
				best.chromosomeID = result.chromosomeID;
			}
				
			//if((i*100/cnt)%5 == 0){
			//	System.out.println(i*100/cnt);
			//}
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());

		System.out.format("%f (%s)\n",best.cost,Long.toBinaryString(best.chromosomeID));
		return null;
		
	}
	private long[] getCombolist(long combo) {
		long combos[] = new long[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = (combo>>(Machine.compList.length*i))&((int)Math.pow(2,Machine.compList.length)-1);
		}
		return combos;
	}
}
