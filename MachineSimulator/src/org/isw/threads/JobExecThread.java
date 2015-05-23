package org.isw.threads;

import org.isw.Component;
import org.isw.Job;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;

public class JobExecThread extends Thread{
	Schedule jobList;
	public JobExecThread(Schedule jobList){
		this.jobList = jobList;
	}
	public void run(){
	int sum=0;

	while(!jobList.isEmpty() && sum < 8*Macros.TIME_SCALE_FACTOR){
		jobList.decrement(1);
		Job current = jobList.peek(); 
		switch(current.getJobType()){
			case Job.JOB_NORMAL:
				Machine.procCost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				for(Component comp : Machine.compList)
					comp.initAge++;
				break;
			case Job.JOB_PM:
				for(int i =0; i<Machine.compList.length;i++){
					if( ((1<<i)&current.getCompCombo()) == 1){
					Machine.pmCost[i] += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
					current.setFixedCost(0);
					}
				}
				
				Machine.downTime++;
				break;
			case Job.JOB_CM:
				Machine.cmCost[current.getCompNo()] += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				Machine.downTime++;
				break;
			case Job.WAIT_FOR_MT:
				Machine.downTime++;
		}
		if(current.getJobTime()<=0){
			//Job ends here
			switch(current.getJobType()){
			case Job.JOB_PM:
				for(int i =0; i<Machine.compList.length;i++){
					if( ((1<<i)&current.getCompCombo()) == 1){
						Component comp = Machine.compList[i];
						comp.initAge = (1-comp.pmRF)*comp.initAge;
					}
				}
				break;
			case Job.JOB_CM:	
				Component comp = Machine.compList[current.getCompNo()];
				comp.initAge = (1 - comp.cmRF)*comp.initAge;
				break;
			case Job.JOB_NORMAL:
				Machine.jobsDone++;
			}
			
			System.out.println("Job "+ jobList.remove().getJobName()+" complete");
		}
		sum++;
		
	}
	if(jobList.isEmpty())
		return;
	int i = jobList.indexOf(jobList.peek());
	while(i < jobList.getSize()){
		Machine.penaltyCost += jobList.jobAt(i++).getPenaltyCost();
	}
	}
	

}