package org.isw.threads;

import java.io.IOException;
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
	int pmOpportunity;
	int noOfSimulations = 1000;

	public SimulationThread(Schedule schedule, int compCombo, int pmOpportunity){
		this.schedule = schedule;
		this.compCombo = compCombo;
		this.pmOpportunity = pmOpportunity;

		}
	/**
	 * We shall run the simulation 1000 times,each simulation being 8 hours (real time) in duration.
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
				if(cmTTF < 8*Macros.TIME_SCALE_FACTOR){
					long cmTTR = (long)(simCompList[i].getCMTTR()*Macros.TIME_SCALE_FACTOR);
					//Smallest unit is one hour for now
					if(cmTTR==0)
						cmTTR=1;
					Job cmJob = new Job("CM",cmTTR,simCompList[i].getCMCost(), Job.JOB_CM);
					cmJob.setFixedCost(simCompList[i].getCompCost());;
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
			while(time< 8*Macros.TIME_SCALE_FACTOR && !simSchedule.isEmpty()){
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
						pmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
						current.setFixedCost(0);
						pmAvgTime += 1;
						break;
					case Job.JOB_CM:
						cmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
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
			   penaltyCost += simSchedule.remove().getPenaltyCost()*8;
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
		return new SimulationResult(totalCost,pmAvgTime,compCombo,pmOpportunity);
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
				Job pmJob = new Job("PM",pmttr,simCompList[i].getPMCost(),Job.JOB_PM);
				if(cnt==0){
					pmJob.setFixedCost(simCompList[i].getPMFixedCost());
					pmJob.setCompCombo(compCombo);
				}
					simSchedule.addPMJob(pmJob,pmOpportunity+cnt);
				cnt++;
			}
		}
	}

}
