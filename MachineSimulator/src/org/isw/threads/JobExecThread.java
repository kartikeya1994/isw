package org.isw.threads;

import org.isw.Job;
import org.isw.Schedule;

public class JobExecThread extends Thread{
	Schedule jobList;
	public JobExecThread(Schedule jobList){
		this.jobList = jobList;
	}
	public void run(){
	int sum=0;
	long downTime = 0;
	long procCost =0;
	long pmCost = 0;
	long cmCost = 0;
	while(!jobList.isEmpty() && sum < 8*60){
		jobList.decrement(1);
		Job current = jobList.peek(); 
		switch(current.getJobType()){
			case Job.JOB_NORMAL:
				procCost += current.getJobCost()/60;
				break;
			case Job.JOB_PM:
				pmCost += current.getFixedCost() + current.getJobCost()/60;
				current.setFixedCost(0);
				downTime++;
				break;
			case Job.JOB_CM:
				cmCost += current.getFixedCost() + current.getJobCost()/60;
				current.setFixedCost(0);
				downTime++;
				break;
			case Job.WAIT_FOR_MT:
				downTime++;
		}
		if(current.getJobTime()<0){
			//Job ends here
			System.out.println("Job "+ jobList.remove().getJobName()+" complete");
		}
		sum++;
	}
	}
	

}