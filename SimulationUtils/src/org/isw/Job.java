package org.isw;

import java.io.Serializable;

public class Job implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	long jobTime;
	String jobName;
	public Job(long jobTime, String jobName) {
		this.jobTime = jobTime;
		this.jobName = jobName;
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
	
	
}
