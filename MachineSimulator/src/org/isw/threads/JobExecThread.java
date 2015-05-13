package org.isw.threads;

import org.isw.JobList;

public class JobExecThread extends Thread{
	JobList jobList;
	public JobExecThread(JobList jobList){
		this.jobList = jobList;
	}
	public void run(){
	
	long startTime = System.currentTimeMillis();
	long time = startTime;
	long time2;
	while(time - startTime < 8*3600 && !jobList.isEmpty()){
			time2 = System.currentTimeMillis();
			jobList.decrement(time2-time);
			time = time2;
			if(jobList.peek().getJobTime()<0){
				System.out.println("Job "+ jobList.remove().getJobName()+" complete");
				}
		}
	System.out.println("Shift over");
	}
}
