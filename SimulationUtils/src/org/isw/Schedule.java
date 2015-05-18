package org.isw;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.LinkedList;



public class Schedule implements  Comparable<Schedule>,Serializable{

	
	private static final long serialVersionUID = 1L;
	LinkedList<Job> jobs;
	private long sum;
	InetAddress ip;

	public Schedule(){
		sum = 0;
		jobs = new LinkedList<Job>();
	}
	
	public Schedule(Schedule source){
		sum = source.sum;
		jobs = new LinkedList<Job>(source.jobs);
	}
	
	public Schedule(InetAddress byName) {
		//check this
		sum = 0;
		jobs = new LinkedList<Job>();
	}

	public void addJob(Job job){
		jobs.add(job);
		sum+=job.getJobTime();
	}
	/**
	 * Add CM Job. If a CM job overlaps with a normal job, split the normal job and insert CM job 
	 * in between. If a CM job overlaps with a PM/CM job, add it after the CM/PM job.
	**/
	public void addCMJob(Job cmJob, long TTF){
		if (TTF >= sum)
			return;
		long time=0;
		int i=0;
		while(time<TTF){
			time+= jobs.get(i).getJobTime();
			i++;
		}
		if(TTF == time || TTF == 0){
			jobs.add(i, cmJob);
		}
		else{
			if(jobs.get(i-1).getJobType() == Job.JOB_NORMAL){
				i--;
				Job job  = jobs.remove(i);
				Job j1 =  new Job(job.getJobName(),TTF-time+job.getJobTime(),job.getJobCost(),Job.JOB_NORMAL);
				Job j2 = new Job(job.getJobName(),time-TTF,job.getJobCost(),Job.JOB_NORMAL);
				jobs.add(i,j1);
				jobs.add(i+1,cmJob);
				jobs.add(i+2,j2);
			}
			else{
				jobs.add(i,cmJob);
			}
		}
		sum+=cmJob.getJobTime();
	}
	//Insert PM job at give opportunity.
	public void addPMJob(Job pmJob, int opportunity){
		jobs.add(opportunity, pmJob);
		sum+=pmJob.getJobTime();
	}
	
	public synchronized Job remove(){
		Job job = jobs.removeFirst();
		sum-= job.getJobTime();
		return job;
	}
	
	@Override
	public int compareTo(Schedule other) {
		return Long.compare(this.sum, other.sum);
	}

	public synchronized boolean isEmpty() {
		return jobs.isEmpty();
	}
	
	public synchronized Job peek(){
		return jobs.getFirst();
	}
	
	public synchronized void decrement(long delta){
		jobs.getFirst().decrement(delta);
		sum-= delta;
	}

	public String printSchedule() {
		String str="";
		for(int i=0;i<jobs.size();i++)
			str += jobs.get(i).getJobName()+": "+ jobs.get(i).getJobTime()/60+" ";
				
		return str;
	}
	public int getFarthestCompleteJob(){
		int i = jobs.size();
		long temp = sum;
		while(temp>8*3600){
			temp -= jobs.get(i-1).getJobTime();
			i--;
		}
		return i;
	}

	//check these two
	public InetAddress getAddress() {
		// TODO Auto-generated method stub
		return ip;
	}

	public void setAddress(InetAddress ip) {
		this.ip = ip;
		
	}

}