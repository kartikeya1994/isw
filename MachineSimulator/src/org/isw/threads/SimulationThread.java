package org.isw.threads;

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
		System.out.println("CompCombo: "+ compCombo+ " Pm opportunity "+pmOpportunity);
		while(cnt++ <= noOfSimulations){
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
				long cmTTF = (long)simCompList[i].getCMTTF()*Macros.TIME_SCALE_FACTOR;
				if(cmTTF < 8*Macros.TIME_SCALE_FACTOR){
					Job cmJob = new Job("CM", (long)simCompList[i].getCMTTR()*Macros.TIME_SCALE_FACTOR,simCompList[i].getCMCost(), Job.JOB_CM);
					cmJob.setFixedCost(simCompList[i].getCompCost());;
					simSchedule.addCMJob(cmJob, cmTTF);
				}
			}
			long startTime = System.currentTimeMillis();
			long time = startTime;
			long time2 = startTime;
			while(time - startTime < 8*Macros.TIME_SCALE_FACTOR && !simSchedule.isEmpty()){
				time2 = System.currentTimeMillis();
				simSchedule.decrement(time2-time);
				//Calculate the cost depending upon the job type
				Job current = simSchedule.peek(); 
				switch(current.getJobType()){
					case Job.JOB_NORMAL:
						procCost += current.getJobCost()*(time2-time)/Macros.TIME_SCALE_FACTOR;
						break;
					case Job.JOB_PM:
						pmCost += current.getFixedCost() + current.getJobCost()*(time2-time)/Macros.TIME_SCALE_FACTOR;
						current.setFixedCost(0);
						pmAvgTime += time2-time;
						break;
					case Job.JOB_CM:
						cmCost += current.getFixedCost() + current.getJobCost()*(time2-time)/Macros.TIME_SCALE_FACTOR;
						current.setFixedCost(0);
						break;
				}
				if(current.getJobTime()<0){
					//Job ends here
					//System.out.println("Job "+ simSchedule.remove().getJobName()+" complete");
				}
				time = time2;
			}
			//Calculate penaltyCost for leftover jobs
		   while(!simSchedule.isEmpty()){
			   penaltyCost += simSchedule.remove().getJobCost()*8;
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
		String formatPattern = "%" + simCompList.length + "s";
		String combo = String.format(formatPattern, Integer.toBinaryString(compCombo)).replace(' ', '0');
		for(int i=0;i< combo.length();i++){
			if(combo.charAt(i)=='1'){
				Job pmJob = new Job("PM",(long)simCompList[i].getPMTTR()*60,simCompList[i].getPMCost(),Job.JOB_PM);
				if(cnt==0)
					pmJob.setFixedCost(simCompList[i].getPMFixedCost());
				simSchedule.addPMJob(pmJob,pmOpportunity+cnt);
				cnt++;
			}
		}
	}

}
