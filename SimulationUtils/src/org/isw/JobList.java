package org.isw;

import java.io.Serializable;
import java.net.InetAddress;



public class JobList implements  Comparable<JobList>,Serializable{

	private static final long serialVersionUID = 1L;
	private Job[] jobs;
	private long sum;
	private int front;
	private int back;
	private InetAddress ip;
	public JobList(InetAddress ip){
		sum = 0;
		front = back = 0;
		this.ip = ip;
		jobs = new Job[20];
	}
	
	public InetAddress getAddress(){
		return ip;
	}
	
	public void addJob(Job job){
		jobs[back] = job;
		back = (back+1)%20;
		sum+=job.getJobTime();
	}
	
	public synchronized Job remove(){
		Job job = jobs[front];
		sum-= job.getJobTime();
		front=(front+1)%20;
		if(front == back)
			front = back = 0;
		return job;
	}
	
	@Override
	public int compareTo(JobList other) {
		return Long.compare(this.sum, other.sum);
	}

	public synchronized boolean isEmpty() {
		return back-front==0;
	}
	public synchronized Job peek(){
		return jobs[front];
	}
	public synchronized void decrement(long delta){
		jobs[front].decrement(delta);
		sum-= delta;
	}

	public void setAddress(InetAddress ip) {
		this.ip = ip; 
		
	}

	public String printList() {
		String str="";
		for(int i=front;i<back;i++){
			str += jobs[i].getJobName()+": "+ jobs[i].getJobTime()/60+" ";
		}
		return str;
	}
   
    }