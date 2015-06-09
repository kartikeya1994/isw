package org.isw;

import java.io.Serializable;

public class Job implements Serializable {
	/**
	 * 
	 */
	public static final int JOB_NORMAL = 1;
	public static final int JOB_PM = 2;
	public static final int JOB_CM = 3;
	public static final int WAIT_FOR_MT = 4;
	private static final long serialVersionUID = 1L;
	long jobTime;
	long dueAfter; // hours that the job is due after, -1 if its a CM or PM job
	String jobName;
	int jobType;
	int compNo;
	int compCombo;

	double fixedCost; //fixed cost for CM or PM
	double compCost; //component cost in case of CM or PM job
	
	double jobCost; // cost per hour for CM or PM, or job processing cost
	double penaltyCost;
	public Job(String jobName, long jobTime,double  jobCost, int jobType) {
		this.jobTime = jobTime;
		this.jobName = jobName;
		this.jobType = jobType;
		this.jobCost = jobCost;
		fixedCost = 0;
		penaltyCost = 0;
		compCost = 0;
	}
	
	public Job(String jobName, long jobTime,double jobCost, double fixedCost, double compCost, int jobType) {
		//to initialize CM job
		this.jobTime = jobTime;
		this.jobName = jobName;
		this.jobType = jobType;
		this.jobCost = jobCost;
		this.fixedCost = fixedCost;
		penaltyCost = 0;
		this.compCost = compCost;
	}
	
	public Job(Job source) {
		this.jobTime = source.jobTime;
		this.jobName = source.jobName;
		this.jobType = source.jobType;
		this.jobCost = source.jobCost;
		compCombo = source.compCombo;
		compNo = source.compNo;
		fixedCost = source.fixedCost;
		penaltyCost = source.penaltyCost;
		this.compCost = source.compCost;
	
	}

	public void setPenaltyCost(double penaltyCost){
		this.penaltyCost = penaltyCost;
	}
	public double getPenaltyCost(){
		return penaltyCost;
	}
	public long getJobTime() {
		return jobTime;
	}
	
	public void decrement(long delta) {
		jobTime -=delta;
		if(jobTime<0)
			jobTime=0;
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public int getJobType(){
		return jobType;
	}
	
	/**
	 * Fixed cost flag.
	 * TODO: Rename to something better.
	 * **/
	public void setFixedCost(double fixedCost){
		this.fixedCost = fixedCost;
	}
	public double getFixedCost(){
		return fixedCost;
	}

	public double getJobCost() {
		return jobCost;
	}
	public int getCompNo(){
		return compNo;
	}
	public void setCompNo(int no ){
		compNo = no;
		
	}
	public int getCompCombo() {
		return compCombo;
	}

	public void setCompCombo(int compCombo) {
		this.compCombo = compCombo;
	}
	public void setCompCost(double compCost){
		this.compCost = compCost;
	}
	
	public double getCompCost(){
		return compCost;
	}
	
}
