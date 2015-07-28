package org.isw.threads;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.isw.Component;
import org.isw.Job;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;
import org.isw.SimulationResult;

public class SimulationThread implements Callable<SimulationResult> {
	Schedule schedule; // Job Schedule received by scheduler
	int compCombo; //Combination of components to perform PM on.
	int pmLabour[]; //Number of labourers required.
	int pmOpportunity;
	int noOfSimulations = Macros.SIMULATION_COUNT;

	public SimulationThread(Schedule schedule, int compCombo, int pmOpportunity){
		this.schedule = schedule;
		this.compCombo = compCombo;
		this.pmOpportunity = pmOpportunity;
		this.pmLabour = new int[3];
	}
	/**
	 * We shall run the simulation 1000 times,each simulation being Macros.SHIFT_DURATION hours (real time) in duration.
	 * For each simulation PM is done only once and is carried out in between job executions.
	 * **/
	public SimulationResult call(){
		double totalCost = 0;
		double pmAvgTime = 0;
		int cnt = 0;
		//System.out.println("CompCombo: "+ compCombo+ " Pm opportunity "+pmOpportunity);
		while(cnt++ < noOfSimulations){
			double procCost = 0;  //Processing cost
			double pmCost = 0;   //PM cost 
			double cmCost = 0;   //CM cost
			double penaltyCost = 0; //Penalty cost
			Schedule simSchedule = new Schedule(schedule);
			Component[] simCompList = Machine.compList.clone();
			/*Add PM job to the schedule*/
			if(pmOpportunity >=0 ){
				addPMJobs(simSchedule,simCompList);
			}
			/*Calculate the TTF for every component and add it's corresponding CM job 
			 * to the schedule*/
			for(int i=0;i<simCompList.length;i++){
				long cmTTF = (long)(simCompList[i].getCMTTF()*Macros.TIME_SCALE_FACTOR);
				if(cmTTF < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR){
					ArrayList<Job> pmJobs =	simSchedule.getPMJobs();
					boolean flag = false;
					for(Job pmJob : pmJobs){
					int tempIndex = simSchedule.indexOf(pmJob);
					int compCombo = pmJob.getCompCombo();
					long time =	schedule.getFinishingTime(tempIndex-1);
					if(cmTTF>= time && ((1<<i)&compCombo)!=0)
						flag = true;
					}
					if(flag)
						continue;
				
					long cmTTR = (long)(simCompList[i].getCMTTR()*Macros.TIME_SCALE_FACTOR);
					//Smallest unit is one hour for now
					if(cmTTR==0)
						cmTTR=1;
					Job cmJob = new Job("CM",cmTTR,simCompList[i].getCMLabourCost(),Job.JOB_CM);
					cmJob.setFixedCost(simCompList[i].getCMFixedCost());
					cmJob.setCompNo(i);
					try{
						simSchedule.addCMJob(cmJob, cmTTF);
					}
					catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			long time = 0;
			while(time< Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR && !simSchedule.isEmpty()){
				try{
					simSchedule.decrement(1);
				}
				catch(IOException e){
					e.printStackTrace();
					System.exit(0);
				}
				//Calculate the cost depending upon the job type
				Job current = simSchedule.peek(); 
				switch(current.getJobType()){
				case Job.JOB_NORMAL:
					procCost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
					break;
				case Job.JOB_PM:
					pmCost += current.getFixedCost() + current.getJobCost()*current.getJobTime()/Macros.TIME_SCALE_FACTOR;
					current.setFixedCost(0);
					pmAvgTime += 1;
					break;
				case Job.JOB_CM:
					cmCost += current.getFixedCost() + current.getJobCost()*current.getJobTime()/Macros.TIME_SCALE_FACTOR;
					current.setFixedCost(0);
					break;
				}

				if(current.getJobTime()<=0){
					try{
						simSchedule.remove();
					}
					catch(IOException e){
						e.printStackTrace();
						System.exit(0);
					}
				}
				time++;
			}
			try{
				//Calculate penaltyCost for leftover jobs
				while(!simSchedule.isEmpty()){
					penaltyCost += simSchedule.remove().getPenaltyCost()*Macros.SHIFT_DURATION;
				}
			}
			catch(IOException e){
				e.printStackTrace();
				System.exit(0);
			}
			//Calculate totalCost for the shift
			totalCost += procCost + pmCost + cmCost + penaltyCost;

		}
		totalCost /= noOfSimulations;
		pmAvgTime /= noOfSimulations;
		return new SimulationResult(totalCost,pmAvgTime,compCombo,pmOpportunity,pmLabour);
	}
	/*Add PM job for the given combination of components.
	 * The PM jobs are being split into smaller PM jobs for each component.
	 * But the fixed PM cost is only added for the first job to meet the cost model requirements.
	 * */
	private void addPMJobs(Schedule simSchedule,Component[] simCompList) {
		int cnt = 0;
		for(int i=0;i< simCompList.length;i++){
			int pos = 1<<i;
			if((pos&compCombo)!=0){
				long pmttr = (long)(simCompList[i].getPMTTR()*Macros.TIME_SCALE_FACTOR);
				//Smallest unit is one hour
				if(pmttr == 0)
					pmttr=1;
				Job pmJob = new Job("PM",pmttr,simCompList[i].getPMLabourCost(),Job.JOB_PM);
				pmJob.setCompCombo(compCombo);
				pmLabour[0]+= simCompList[i].getPMLabour()[0];
				pmLabour[1]+= simCompList[i].getPMLabour()[1];
				pmLabour[2]+= simCompList[i].getPMLabour()[2];
				if(cnt==0){
					pmJob.setFixedCost(simCompList[i].getPMFixedCost());
				}
				simSchedule.addPMJob(pmJob,pmOpportunity+cnt);
				cnt++;
			}
		}
	}

}
