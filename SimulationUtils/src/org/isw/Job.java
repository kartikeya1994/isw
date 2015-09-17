package org.isw;

import java.io.Serializable;

public class Job implements Serializable {
	/**
	 * 
	 */
	public static final int JOB_NORMAL = 1;
	public static final int JOB_PM = 2;
	public static final int JOB_CM = 3;
	
	// job status
	public static final int NOT_STARTED=1;
	public static final int STARTED = 2;
	public static final int SERIES_STARTED = 3;
	
	private static final long serialVersionUID = 1L;
	long jobTime;
	String jobName;
	int jobType;
	int compNo; // in case of CM
	int jobStatus;
	
	double fixedCost; //fixed cost for CM or PM
	
	long seriesTTR;
	int[] seriesLabour;
	
	double jobCost; // cost per hour for CM or PM, or job processing cost
	double penaltyCost;
	public Job(String jobName, long jobTime,double  jobCost, int jobType) {
		this.jobTime = jobTime;
		this.jobName = jobName;
		this.jobType = jobType;
		this.jobCost = jobCost;
		this.jobStatus = NOT_STARTED;
		fixedCost = 0;
		penaltyCost = 0;
	}
	
	public Job(Job source) {
		this.jobTime = source.jobTime;
		this.jobName = source.jobName;
		this.jobType = source.jobType;
		this.jobCost = source.jobCost;
		this.jobStatus = source.jobStatus;
		compNo = source.compNo;
		fixedCost = source.fixedCost;
		penaltyCost = source.penaltyCost;
		seriesTTR = source.seriesTTR;
		seriesLabour = source.seriesLabour;
	
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
	}
	
	public String getJobName() {
		return jobName;
	}
	
	public int getJobType(){
		return jobType;
	}
	
	public void setStatus(int status){
		this.jobStatus = status;
	}
	
	public int getStatus()
	{
		return this.jobStatus;
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
	
	public long getSeriesTTR() {
		return seriesTTR;
	}

	public void setSeriesTTR(long seriesTTR) {
		this.seriesTTR = seriesTTR;
	}

	public int[] getSeriesLabour() {
		return seriesLabour;
	}

	public void setSeriesLabour(int[] seriesLabour) {
		this.seriesLabour = seriesLabour;
	}
	
		
}
