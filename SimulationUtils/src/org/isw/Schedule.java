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
		setSum(0);
		jobs = new LinkedList<Job>();
	}
	
	public Schedule(Schedule source){
		setSum(source.getSum());
		jobs = new LinkedList<Job>(source.jobs);
	}
	
	public Schedule(InetAddress byName) {
		//check this
		setSum(0);
		jobs = new LinkedList<Job>();
	}

	public void addJob(Job job){
		jobs.add(job);
		setSum(getSum() + job.getJobTime());
	}
	
	public int numOfJobs()
	{
		return jobs.size();
	}
	/**
	 * Add CM Job. If a CM job overlaps with a normal job, split the normal job and insert CM job 
	 * in between. If a CM job overlaps with a PM/CM job, add it after the CM/PM job.
	**/
	public void addCMJob(Job cmJob, long TTF){
		if (TTF >= getSum())
			return;
		long time=0;
		int i=0;
		while(time<TTF){
			time+= jobs.get(i).getJobTime();
			i++;
		}
		if(TTF == time || TTF == 0){
			while(jobs.get(i).getJobType() == Job.JOB_PM)
				i++;
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
				while(jobs.get(i).getJobType() == Job.JOB_PM)
					i++;
				jobs.add(i,cmJob);
			}
		}
		setSum(getSum() + cmJob.getJobTime());
	}
	//Insert PM job at give opportunity.
	public void addPMJob(Job pmJob, int opportunity){
		jobs.add(opportunity, pmJob);
		setSum(getSum() + pmJob.getJobTime());
	}
	
	public synchronized Job remove(){
		Job job = jobs.removeFirst();
		setSum(getSum() - job.getJobTime());
		return job;
	}
	
	@Override
	public int compareTo(Schedule other) {
		return Long.compare(this.getSum(), other.getSum());
	}

	public synchronized boolean isEmpty() {
		return jobs.isEmpty();
	}
	
	public synchronized Job peek(){
		return jobs.getFirst();
	}
	
	public synchronized void decrement(long delta){
		jobs.getFirst().decrement(delta);
		setSum(getSum() - delta);
	}

	public String printSchedule() {
		String str="";
		for(int i=0;i<jobs.size();i++)
			str += jobs.get(i).getJobName()+": "+ jobs.get(i).getJobTime()/60+" ";
				
		return str;
	}
	public int getFarthestCompleteJob(){
		int i = jobs.size();
		long temp = getSum();
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

	public long getSum() {
		return sum;
	}

	public void setSum(long sum) {
		this.sum = sum;
	}

	public Job jobAt(long time) {
		int temp =0;
		int i = 0;
		while(temp<time){
			temp+= jobs.get(i).getJobTime();
			i++;
		}
		return jobs.get(i-1);
	}
	
}