package org.isw.threads;

import org.isw.Component;
import org.isw.Job;
import org.isw.Schedule;

public class SimulationThread extends Thread {
	Schedule schedule; // Job Schedule received by scheduler
	int compCombo; //Combination of components to perform PM on.
	int pmOpportunity;
	int noOfSimulations = 1000;
	Component[] compList; 
	public SimulationThread(Schedule schedule, int compCombo, int pmOpportunity, Component[] compList){
		this.schedule = schedule;
		this.compCombo = compCombo;
		this.pmOpportunity = pmOpportunity;
		this.compList = compList;
		}
	/**
	 * We shall run the simulation 1000 times,each simulation being 8 hours (real time) in duration.
	 * For each simulation PM is done only once and is carried out in between job executions.
	 * TODO: Fix time scaling at cost calculations.
	 * **/
	public void run(){
		long totalCost = 0;
		while(noOfSimulations-- <= 0){
			double procCost = 0;  //Processing cost
			double pmCost = 0;   //PM cost 
			double cmCost = 0;   //CM cost
			double penaltyCost = 0; //Penalty cost
			Schedule simSchedule = new Schedule(schedule);
			Component[] simCompList = compList.clone();
			/*Add PM job to the schedule*/
			if(pmOpportunity >=0 ){
				addPMJobs(simSchedule,simCompList);
			}
			/*Calculate the TTF for every component and add it's corresponding CM job 
			 * to the schedule*/
			for(int i=0;i<compList.length;i++){
				long cmTTF = (long)simCompList[i].getCMTTF();
				if(cmTTF < 8*3600){
					Job cmJob = new Job("CM", (long)simCompList[i].getCMTTR(),simCompList[i].getCMCost(), Job.JOB_CM);
					cmJob.setFixedCost(simCompList[i].getCompCost());
					simSchedule.addCMJob(cmJob, cmTTF);
				}
			}
			long startTime = System.currentTimeMillis();
			long time = startTime;
			long time2 = startTime;
			while(time - startTime < 8*3600 && !simSchedule.isEmpty()){
				time2 = System.currentTimeMillis();
				simSchedule.decrement(time2-time);
				Job current = simSchedule.peek(); 
				switch(current.getJobType()){
					case Job.JOB_NORMAL:
						procCost += current.getJobCost()*(time2-time);
						break;
					case Job.JOB_PM:
						pmCost += current.getFixedCost() + current.getJobCost()*(time2-time);
						current.setFixedCost(0);
						break;
					case Job.JOB_CM:
						cmCost += current.getFixedCost() + current.getJobCost()*(time2-time);
						current.setFixedCost(0);
						break;
				}
				if(current.getJobType()<0){
					System.out.println("Job "+ simSchedule.remove().getJobName()+" complete");
				}
				time = time2;
			}
		   while(!simSchedule.isEmpty()){
			   penaltyCost += simSchedule.remove().getJobCost()*8;
		   }
		totalCost += procCost + pmCost + cmCost + penaltyCost;
		}
		
	}
	private void addPMJobs(Schedule simSchedule,Component[] simCompList) {
		int cnt = 0;
		String combo = Integer.toBinaryString(compCombo);
		for(int i=0;i< combo.length();i++){
			if(combo.charAt(i)=='1'){
				Job pmJob = new Job("PM",(long)simCompList[i].getPMTTR(),simCompList[i].getPMCost(),Job.JOB_PM);
				if(cnt==0)
					pmJob.setFixedCost(simCompList[i].getPMFixedCost());
				simSchedule.addPMJob(pmJob,pmOpportunity+cnt);
				cnt++;
			}
		}
	}

}
