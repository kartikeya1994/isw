package org.isw;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Schedule implements  Comparable<Schedule>,Serializable{

	
	private static final long serialVersionUID = 1L;
	ArrayList<Job> jobs;
	private long sum;
	InetAddress ip;

	public Schedule(){
		setSum(0);
		jobs = new ArrayList<Job>();
	}
	
	public Schedule(Schedule source){
		setSum(source.getSum());
		jobs =  new ArrayList<Job>();
		for(int i =0; i<source.jobs.size();i++){
			jobs.add(new Job(source.jobAt(i)));
		}		
	}
	
	public Schedule(InetAddress byName) {
		//check this
		setSum(0);
		jobs = new ArrayList<Job>();
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
		int i = jobIndexAt(TTF);
		long time = getFinishingTime(i);
		if(jobs.get(i).getJobType() == Job.JOB_NORMAL){
				Job job  = jobs.remove(i);
				time -= job.getJobTime();
				Job j1 =  new Job(job.getJobName(),TTF-time,job.getJobCost(),Job.JOB_NORMAL);
				Job j2 = new Job(job.getJobName(),time+job.getJobTime()-TTF,job.getJobCost(),Job.JOB_NORMAL);
				jobs.add(i,j1);
				jobs.add(i+1,cmJob);
				jobs.add(i+2,j2);
			}
			else{
				while(i< jobs.size() && jobs.get(i).getJobType() == Job.JOB_PM)
					i++;
				jobs.add(i,cmJob);
			}
		
		setSum(getSum() + cmJob.getJobTime());
	}
	//Insert PM job at give opportunity.
	public void addPMJob(Job pmJob, int opportunity){
		jobs.add(opportunity, pmJob);
		setSum(getSum() + pmJob.getJobTime());
	}
	
	public synchronized Job remove(){
		Job job = jobs.remove(0);
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
		return jobs.get(0);
	}
	
	public synchronized void decrement(long delta){
		jobs.get(0).decrement(delta);
		setSum(getSum() - delta);
	}

	public String printSchedule() {
		String str="";
		for(int i=0;i<jobs.size();i++)
			str += jobs.get(i).getJobName()+": "+ jobs.get(i).getJobTime()/Macros.TIME_SCALE_FACTOR+"hrs ";
				
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

	public int jobIndexAt(long time) {
		int temp =0;
		int i = 0;
		while(temp<=time){
			temp+= jobs.get(i).getJobTime();
			i++;
		}
		return i-1;
	}

	public void addWaitJob(long startTime, long waitTime, int pmJobIndex) {
		Job pmJob = jobs.remove(pmJobIndex);
		long time = getFinishingTime(pmJobIndex-1);
		Job pmJob1 = new Job("PM",startTime - time,pmJob.getJobCost(),Job.JOB_PM);
		pmJob1.setFixedCost(pmJob.getFixedCost());
		Job waitJob = new Job("Waiting",waitTime,0,Job.WAIT_FOR_MT);
		Job pmJob2 = new Job("PM",time+pmJob.getJobTime()-startTime,pmJob.getJobCost(),Job.JOB_PM);
		jobs.add(pmJobIndex,pmJob1);
		jobs.add(pmJobIndex+1,waitJob);
		jobs.add(pmJobIndex+2,pmJob2);
	}

	public long getFinishingTime(int index){
		return 0;
	}
	public Job jobAt(int i) {
		return jobs.get(i);
	}
	
	public static Schedule receive(ServerSocket tcpSocket)
	{
		//uses TCP to receive Schedule
		Schedule ret = null;
		try
		{
			Socket tcpSchedSock = tcpSocket.accept();
			ObjectInputStream ois = new ObjectInputStream(tcpSchedSock.getInputStream());
			Object o = ois.readObject();

			if(o instanceof Schedule) 
			{

				ret = (Schedule)o;
			}
			else 
			{
				System.out.println("Received Schedule is garbled");
			}
			ois.close();
			tcpSchedSock.close();
		}catch(Exception e)
		{
			System.out.println("Failed to receive schedule.");
		}

		return ret;
	}
}