package org.isw.threads;

import java.io.IOException;

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

	while(!jobList.isEmpty() && sum < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR){
		
		Job current = jobList.peek(); 
		try{
		jobList.decrement(1);
		}
		catch(IOException e){
			e.printStackTrace();
			System.exit(0);
		}
		switch(current.getJobType()){
			case Job.JOB_NORMAL:
				Machine.procCost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				for(Component comp : Machine.compList)
					comp.initAge++;
				break;
			case Job.JOB_PM:
				Machine.pmCost += current.getFixedCost() + current.getJobCost()*current.getJobTime()/Macros.TIME_SCALE_FACTOR;
				Machine.pmDownTime++;
				Machine.downTime++;
				
				break;
			case Job.JOB_CM:
				Machine.cmCost += current.getCompCost() + current.getFixedCost() + current.getJobCost()*current.getJobTime()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				Machine.downTime++;
				Machine.cmDownTime++;
				break;
			case Job.WAIT_FOR_MT:
				Machine.downTime++;
				Machine.waitTime++;
		}
		if(current.getJobTime()<=0){
			//Job ends here
			switch(current.getJobType()){
			case Job.JOB_PM:
				for(int i =0; i<Machine.compList.length;i++){
					int pos=1<<i;
					int bitmask = current.getCompCombo();
					if((pos&bitmask) != 0){
						Component comp = Machine.compList[i];
						comp.initAge = (1-comp.pmRF)*comp.initAge;
						Machine.compPMJobsDone[i]++;
					}
				}
				Machine.pmJobsDone++;
				break;
			case Job.JOB_CM:	
				Component comp = Machine.compList[current.getCompNo()];
				comp.initAge = (1 - comp.cmRF)*comp.initAge;
				Machine.cmJobsDone++;
				Machine.compCMJobsDone[current.getCompNo()]++;
				break;
			case Job.JOB_NORMAL:
				Machine.jobsDone++;
				break;
			}
			try{
			System.out.println("Job "+ jobList.remove().getJobName()+" complete");
			}
			catch(IOException e){
				e.printStackTrace();
				System.exit(0);
			}
			}
		sum++;
		Machine.runTime++;
		
	}
	if(jobList.isEmpty()){
		Machine.idleTime += Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR - sum;
		return;
		}
	int i = jobList.indexOf(jobList.peek());
	while(i < jobList.getSize()){
		Machine.penaltyCost += jobList.jobAt(i++).getPenaltyCost()*Macros.SHIFT_DURATION;
	}
	
	}
	

}