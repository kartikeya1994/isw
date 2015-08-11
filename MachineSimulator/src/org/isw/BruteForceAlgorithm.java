package org.isw;

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
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		int cnt = 0 ;
		for(int i=1 ; i < Math.pow(2, Machine.compList.length*pmOpportunity.length);i++){
			pool.submit(new SimulationThread(schedule, getCombolist(i),pmOpportunity,false,i));
			cnt++;
		}
		ArrayList<SimulationResult> results = new ArrayList<SimulationResult>();
		for(int i=0;i<cnt;i++){
			SimulationResult result = pool.take().get();
			if(noPM.cost > result.cost){
				results.add(result);
			}
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		SimulationResult[] r = new SimulationResult[results.size()];
		r = results.toArray(r);
		SimulationResult best = Collections.min(results);
		System.out.format("%f (%s)\n",best.cost,Integer.toBinaryString(best.chromosomeID));
		return r;
		
	}
	private int[] getCombolist(int combo) {
		int combos[] = new int[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = (combo>>(Machine.compList.length*i))&((int)Math.pow(2,Machine.compList.length)-1);
		}
		return combos;
	}
}
