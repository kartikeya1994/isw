package org.isw;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.threads.FetchIFTask;
import org.isw.threads.SendScheduleTask;


public class Maintenance 
{
	static MachineList list;
	static boolean recd_list;
	static DatagramSocket udpSocket;
	static ServerSocket tcpSocket;
	static DatagramPacket packetOut;

	static ArrayList<SimulationResult> table;
	static ArrayList<InetAddress> ip = new ArrayList<InetAddress>();
	static ArrayList<Long> port = new ArrayList<Long>();
	static ArrayList<Schedule> schedule = new ArrayList<Schedule>();
	static ArrayList<Component[]> component = new ArrayList<Component[]>();
	
	static int numOfMachines;
	static PriorityQueue<CompTTF> ttfList;
	

	public static void main(String[] args)
	{

		//handle program termination
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				System.out.println("Bye");
				udpSocket.close();

				try 
				{
					tcpSocket.close();
				}catch (IOException e) 
				{
					e.printStackTrace();
				}	
			}
		});

		recd_list = false;

		//create sockets for tcp and udp
		try {
			udpSocket = new DatagramSocket(Macros.MAINTENANCE_DEPT_PORT_TCP);
			tcpSocket = new ServerSocket(Macros.MAINTENANCE_DEPT_PORT_TCP);
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		//create packet
		packetOut = FlagPacket.makePacket(Macros.MAINTENANCE_SCHEDULING_GROUP, Macros.SCHEDULING_DEPT_PORT,Macros.REQUEST_MACHINE_LIST);

		while(true)
			startShift();
	}

	public static void startShift()
	{
		try
		{
			//request machine list from scheduling dept
			while(!recd_list)
			{
				System.out.println("Requesting machine list from scheduling dept...");
				udpSocket.send(packetOut); //UDP

				list = MachineList.receive(tcpSocket);

				if(list != null)
				{
					recd_list=true;
					System.out.println("Received machine list from "+ list.senderIP);
					System.out.println(list);
					//print recd list
				}
				else
					continue;
			}

			//use thread pool to query each machine in list for IFs and schedule
			Enumeration<InetAddress> ips = list.getIPs();
			Enumeration<Long> ports = list.getPorts();
			ExecutorService threadPool = Executors.newFixedThreadPool(5);
			CompletionService<IFPacket> pool = new ExecutorCompletionService<IFPacket>(threadPool);

			numOfMachines = 0;
			while(ips.hasMoreElements()){
				numOfMachines++;
				pool.submit(new FetchIFTask(tcpSocket, ips.nextElement(), ports.nextElement()));
			}

			
			System.out.println("Fetching IFs and schedules from " + numOfMachines + " connected machines...");
			

			int count = 0; //count provides temporary id numbers to machines
			ip = new ArrayList<InetAddress>();
			port = new ArrayList<Long>();
			schedule = new ArrayList<Schedule>();
			component = new ArrayList<Component[]>();
			ttfList = new PriorityQueue<CompTTF>();
			for(int i = 0; i < numOfMachines; i++)
			{
				IFPacket p = pool.take().get(); //fetch results of all tasks
				System.out.println("Machine " + p.ip + "\n" + p);

				ip.add(p.ip);
				port.add(p.port);
				Schedule sched = p.jobList;
				schedule.add(sched);
				component.add(p.compList);
				for(int j=0;j<p.results.length;j++)
				{
					p.results[j].id = count; //assign machine id

					//calculate t for each SimulationResult
					long t = 0;
					for(int k=0; k<p.results[j].pmOpportunity;k++)
						t += sched.jobs.get(k).jobTime;
					p.results[j].t = t; //assign calculated t

					table.add(p.results[j]);
				}
			
				count++;
			}
			threadPool.shutdown();
			while(!threadPool.isTerminated()); //block till all tasks are done
			System.out.println("Collected IFs and schedules from all machines. Incorporating PM in schedule...");

		}catch(Exception e)
		{
			e.printStackTrace();
		}

		//create PM incorporated schedule
		/**
		 * TOOD: Algo is incorrect. pmOpportunity is not time, its slot.
		 */
		long[] pmTimeArray = new long[schedule.size()];
		int[] compCombos = new int[schedule.size()];
		
		Collections.sort(table, new CustomComparator()); //sort according to lower t, higher IF and lower PM time
		int busyTime = 0;
		HashMap<Integer, Boolean> toPerformPM = new HashMap<Integer,Boolean>(); //only one PM per machine per shift.
		for(int i=0;i<table.size();i++)
		{
			SimulationResult row = table.get(i);
			if(row.t >= busyTime && !toPerformPM.containsKey(row.id))
			{
				//incorporate the job into schedule of machine
				schedule.get(row.id).addPMJob(new Job("PM", (long)row.pmAvgTime, row.cost,Job.JOB_PM),row.pmOpportunity);
				toPerformPM.put(row.id, true);
				pmTimeArray[row.id] = row.t;
				compCombos[row.id] = row.compCombo;
				busyTime += (long)row.pmAvgTime;
			}
		}
		for(int i=0; i<numOfMachines;i++){
			for(int j=0;j<component.get(i).length;j++){
				long ttf = (long)component.get(i)[j].getCMTTF();
				//If TTF is greater than shift time or schedule length, ignore.
			if(ttf> 8 || ttf > schedule.get(i).getSum())
				continue;
			//if PM is performed for a component before ttf of that component, ignore.
			if(ttf> pmTimeArray[i] && ((1<<j)&compCombos[i])==1)
				continue;
			long ttr = (long)component.get(i)[j].getCMTTR();
			Job cmJob = new Job("CM",ttr,component.get(i)[j].getCMCost(),Job.JOB_CM);
			cmJob.setFixedCost(component.get(i)[j].getCompCost());
			ttfList.add(new CompTTF(ttf,cmJob,i));	
			
			}
		}
		
		while(!ttfList.isEmpty()){
			CompTTF compTTF = ttfList.remove();
			int index = schedule.get(compTTF.machineID).jobIndexAt(compTTF.ttf);
			Job job = schedule.get(compTTF.machineID).jobAt(index);
			/**If breakdown occurs on a machine while PM/CM is going on shift the ttf to occur after
			 * the PM/CM ends
			 * **/ 
			if(job.getJobType() == Job.JOB_PM || job.getJobType() == Job.JOB_CM)
			{
				//TODO: Calculate new time
				long newTime = schedule.get(compTTF.machineID).getFinishingTime(index);
				ttfList.add(new CompTTF(newTime,compTTF.cmJob,compTTF.machineID));
				continue;
			}
			
			boolean pmFlag = false;
			boolean cmFlag = false;
			int cmIndex = 0;
			int cmJobIndex = 0;
			int pmIndex =0;
			int pmJobIndex =0;
			for(int i = 0;i < schedule.size();i++){
				if(compTTF.machineID == i)
					continue;
				int index1 = schedule.get(i).jobIndexAt(compTTF.ttf); 
				Job job1 = schedule.get(i).jobAt(i);
				switch(job1.getJobType()){
					case Job.JOB_CM:
						cmFlag = true;
						cmIndex = i;
						cmJobIndex = index1;
						break;
					case Job.JOB_PM:
						pmFlag = true;
						pmIndex = i;
						pmJobIndex = index1;
						break;
				}		
			
				if(cmFlag){
					/**If CM is being performed on a different machine wait for CM to complete i.e 
					 * shift breakdown time to occur after CM job.
					 * **/
					long newTime = schedule.get(cmIndex).getFinishingTime(cmJobIndex);
					ttfList.add(new CompTTF(newTime,compTTF.cmJob,compTTF.machineID));
					continue;
				}
				else if(pmFlag){
					/**If PM is being performed on a different machine, interrupt that PM and add a waiting job
					 * and then add the CM job for our machine.
					 **/
					schedule.get(pmIndex).addWaitJob(compTTF.ttf, compTTF.cmJob.getJobTime(),pmJobIndex);
					schedule.get(compTTF.machineID).addCMJob(compTTF.cmJob, compTTF.ttf);
				}
				else{
					//Since no maintenance is going on add CM job directly
					schedule.get(compTTF.machineID).addCMJob(compTTF.cmJob, compTTF.ttf);
				}
			}
			
		
		}
		
		System.out.println("PM + CM incorporated schedule- ");
		for(int i=0; i<numOfMachines;i++)
			System.out.println("Machine: "+ip.get(i)+"\nSchedule: "+schedule.get(i).printSchedule());
		//sending PM incorporated schedule to respective machines
		System.out.println("Sending to all machines...");
		ExecutorService threadPool = Executors.newFixedThreadPool(5);
		for(int x=0; x<ip.size();x++)
		{
			threadPool.execute(new SendScheduleTask(schedule.get(x), ip.get(x), (int)port.get(x).longValue()));
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated()); //block till all tasks are done
		System.out.println("Successfully sent PM incorporated schedules to all connected machines.\nShift can now begin.");
	}
	
	public void incorporateCM()
	{
		ArrayList<Long> tof; //tofs of all machines sorted in ascending order
		ArrayList<String> component; //corresponding component name
		ArrayList<Integer> machineID; //corresponding machine id
		ArrayList<Integer> jobsRemaining = new ArrayList<Integer>();
		
		for(int i=0;i<schedule.size();i++)
			jobsRemaining.add(schedule.get(i).numOfJobs());
		
		long time = 0;
		

		while(!allJobsDone())
		{
			for(int i=0; i<schedule.size(); i++)
			{

			}
		}
	}
	
	public void delayJob(Schedule s, long tstart, long tfinish)
	{
		long t = 0;
		
	}
	
	public void removeComponent(String name, Integer machineID)
	{
		
	}
	
	public boolean allJobsDone()
	{
		return false;
	}
	

}	
class CompTTF implements Comparable<CompTTF>{
	public long ttf;
	public int machineID;
	public Job cmJob;
	public CompTTF(long ttf2,Job cmJob,int count) {
		ttf = ttf2;
		machineID = count;
		this.cmJob = cmJob;
	}
	@Override
	public int compareTo(CompTTF other) {
		return Long.compare(ttf, other.ttf);
	}
	
}