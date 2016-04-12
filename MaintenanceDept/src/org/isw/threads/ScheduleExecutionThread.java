package org.isw.threads;

import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.Machine;
import org.isw.Maintenance;
import org.isw.Schedule;

public class ScheduleExecutionThread implements Runnable{
	ArrayList<Schedule> scheduleList;
	ArrayList<Machine> machineList;
	public ScheduleExecutionThread(ArrayList<Schedule> scheduleList, ArrayList<Machine> machineList){
		this.scheduleList = scheduleList;
		this.machineList = machineList;
	}
	

	@Override
	public void run() {
		ExecutorService threadPool = Executors.newFixedThreadPool(machineList.size());
		CompletionService<Double[]> pool = new ExecutorCompletionService<Double[]>(threadPool);
		CyclicBarrier sync = new CyclicBarrier(machineList.size());
		Object lock = new Object();
		int[] labour = new int[3];
		labour[0] = Maintenance.maxLabour[0];
		labour[1] = Maintenance.maxLabour[1];
		labour[2] = Maintenance.maxLabour[2];
		for(int i=0;i<machineList.size();i++){
			pool.submit(new JobExecThread(scheduleList.get(i),machineList.get(i),false,sync,lock,labour));
		}
		
			for(int i=0;i<machineList.size();i++){
			try {
				Double[] totalCost = pool.take().get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
	}

}
