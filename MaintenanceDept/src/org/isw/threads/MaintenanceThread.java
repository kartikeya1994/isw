
package org.isw.threads;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.Component;
import org.isw.CustomComparator;
import org.isw.IFPacket;
import org.isw.Job;
import org.isw.MachineList;
import org.isw.Macros;
import org.isw.Schedule;
import org.isw.SimulationResult;

public class MaintenanceThread  extends Thread{
	MachineList machineList;
	static DatagramSocket socket;
	static ServerSocket tcpSocket;
	static int[] pmMaxLabour;
	int[] cmMaxLabour;
	static ArrayList<SimulationResult> table = new ArrayList<SimulationResult>();
	static ArrayList<InetAddress> ip = new ArrayList<InetAddress>();
	static ArrayList<Integer> port = new ArrayList<Integer>();
	static ArrayList<Schedule> schedule = new ArrayList<Schedule>();
	static ArrayList<Component[]> component = new ArrayList<Component[]>();
	static int numOfMachines;
	static PriorityQueue<CompTTF> ttfList;
	static ArrayList<LabourAssignment> pmLabourAssignment;
	
	public MaintenanceThread(MachineList machineList){
		this.machineList = machineList;
		
		try {
			socket = new DatagramSocket(Macros.MAINTENANCE_DEPT_PORT);
			tcpSocket = new ServerSocket(Macros.MACHINE_PORT_TCP);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run()
	{
		while(true)
			startShift();
	}
	public void startShift()
	{
		try
		{
			
			//use thread pool to query each machine in list for IFs and schedule
			Enumeration<InetAddress> ips = machineList.getIPs();
			ExecutorService threadPool = Executors.newFixedThreadPool(5);
			CompletionService<IFPacket> pool = new ExecutorCompletionService<IFPacket>(threadPool);

			numOfMachines = 0;
			while(ips.hasMoreElements()){
				numOfMachines++;
				pool.submit(new FetchIFTask(tcpSocket, ips.nextElement(), Macros.MACHINE_PORT_TCP));
			}

			
			System.out.println("Fetching IFs and schedules from " + numOfMachines + " connected machines...");
			
			table = new ArrayList<SimulationResult>();
			ip = new ArrayList<InetAddress>();
			port = new ArrayList<Integer>();
			schedule = new ArrayList<Schedule>();
			component = new ArrayList<Component[]>();
			ttfList = new PriorityQueue<CompTTF>();
			pmLabourAssignment = new ArrayList<LabourAssignment>();
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
					p.results[j].id = i; //assign machine id
					if(p.results[j].pmOpportunity <= 0){
						p.results[j].t = 0; //assign calculated t
					}else{
						p.results[j].t = sched.getFinishingTime(p.results[j].pmOpportunity-1);
					}
					if(p.results[j].pmOpportunity >= 0){
					table.add(p.results[j]);
					}
				}
			}
			threadPool.shutdown();
			while(!threadPool.isTerminated()); //block till all tasks are done
			System.out.println("Collected IFs and schedules from all machines. Incorporating PM in schedule...");

		}catch(Exception e)
		{
			e.printStackTrace();
		}

		//create PM incorporated schedule
		long[] pmTimeArray = new long[schedule.size()];
		int[] compCombos = new int[schedule.size()];

		Collections.sort(table, new CustomComparator()); //sort according to lower t, higher IF and lower PM time

		int busyTime = 0;
		HashMap<Integer, Boolean> toPerformPM = new HashMap<Integer,Boolean>(); //only one PM per machine per shift.
		for(int i=0;i<table.size();i++)
		{
			SimulationResult row = table.get(i);
			if(!toPerformPM.containsKey(row.id) && !schedule.get(row.id).isEmpty())
			{
				checkLabourAssignment(row);
				//incorporate the job into schedule of machine
				long pmtime = addPMJobs(schedule.get(row.id),component.get(row.id),row.compCombo,row.pmOpportunity);
				setLabourAssignment(row,row.t+pmtime);
				toPerformPM.put(row.id, true);
				pmTimeArray[row.id] = row.t;
				compCombos[row.id] = row.compCombo;
				busyTime += pmtime;
			}
		}
		
		for(int i=0; i<numOfMachines;i++){
			for(int j=0;j<component.get(i).length;j++){
				long ttf = (long)(component.get(i)[j].getCMTTF()*Macros.TIME_SCALE_FACTOR);
				//If TTF is greater than shift time or schedule length, ignore.
			if(ttf>= Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR || ttf >= schedule.get(i).getSum())
				continue;
			//if PM is performed for a component before ttf of that component, ignore.
			ArrayList<Job> pmJobs =	schedule.get(i).getPMJobs();
			boolean flag = false;
			for(Job pmJob : pmJobs){
			int tempIndex = schedule.get(i).indexOf(pmJob);
			int compCombo = pmJob.getCompCombo();
			long time =	schedule.get(i).getFinishingTime(tempIndex-1);
			if(ttf>= time && ((1<<j)&compCombo)!=0)
				flag = true;
			}
			if(flag)
				continue;
			long ttr = (long)(component.get(i)[j].getCMTTR()*Macros.TIME_SCALE_FACTOR);
			if(ttr == 0)
				ttr = 1;
			ttfList.add(new CompTTF(ttf,ttr,i,j));	
			}
		}
		
		while(!ttfList.isEmpty()){
			CompTTF compTTF = ttfList.remove();
			System.out.println(compTTF.ttf);
			int index = schedule.get(compTTF.machineID).jobIndexAt(compTTF.ttf);
			Job job = schedule.get(compTTF.machineID).jobAt(index);
			/**If breakdown occurs on a machine while PM/CM is going on shift the ttf to occur after
			 * the PM/CM ends
			 * **/ 
			if(job.getJobType() == Job.JOB_PM || job.getJobType() == Job.JOB_CM)
			{
				//TODO: Calculate new time
				long newTime = schedule.get(compTTF.machineID).getFinishingTime(index);
				if(newTime >= schedule.get(compTTF.machineID).getSum() || newTime >= Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR)
					continue;
				ttfList.add(new CompTTF(newTime,compTTF.ttr,compTTF.machineID,compTTF.componentID));
				continue;
			}
			
			boolean pmFlag = false;
			boolean cmFlag = false;
			int cmIndex = 0;
			int cmJobIndex = 0;
			int pmIndex =0;
			int pmJobIndex =0;
			//Loop through all machines and check for overlaps.
			for(int i = 0;i < schedule.size();i++){
				if(compTTF.machineID == i || schedule.get(i).getSum() <= compTTF.ttf || schedule.get(i).isEmpty())
					continue;
				int index1 = schedule.get(i).jobIndexAt(compTTF.ttf); 
				Job job1 = schedule.get(i).jobAt(index1);
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
					 * FIXME: Ignoring for now.
					 * **/
					long newTime = schedule.get(cmIndex).getFinishingTime(cmJobIndex);
					if(newTime>=Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR||newTime>=schedule.get(compTTF.machineID).getSum())
						continue;
					int jobIndex = schedule.get(compTTF.machineID).jobIndexAt(compTTF.ttf);
					schedule.get(compTTF.machineID).addWaitJob(compTTF.ttf, newTime-compTTF.ttf, jobIndex);
					ttfList.add(new CompTTF(newTime,compTTF.ttr,compTTF.machineID,compTTF.componentID));
				}
				else if(pmFlag){
					/**If PM is being performed on a different machine, interrupt that PM and add a waiting job
					 * and then add the CM job for our machine.
					 **/
					schedule.get(pmIndex).addWaitJob(compTTF.ttf, compTTF.ttr,pmJobIndex);
					Job cmJob = new Job("CM",compTTF.ttr,component.get(compTTF.machineID)[compTTF.componentID].getCMCost(),Job.JOB_CM);
					cmJob.setFixedCost(component.get(compTTF.machineID)[compTTF.componentID].getCompCost());
					cmJob.setCompNo(compTTF.componentID);
					schedule.get(compTTF.machineID).addCMJob(cmJob, compTTF.ttf);
				}
				else{
					//Since no maintenance is going on add CM job directly
					Job cmJob = new Job("CM",compTTF.ttr,component.get(compTTF.machineID)[compTTF.componentID].getCMCost(),
							component.get(compTTF.machineID)[compTTF.componentID].getCMFixedCost(), component.get(compTTF.machineID)[compTTF.componentID].getCompCost(),Job.JOB_CM);
					cmJob.setCompNo(compTTF.componentID);
					schedule.get(compTTF.machineID).addCMJob(cmJob, compTTF.ttf);
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
			threadPool.execute(new SendScheduleTask(schedule.get(x), ip.get(x), Macros.MACHINE_PORT_TCP));
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated()); //block till all tasks are done
		System.out.println("Successfully sent PM incorporated schedules to all connected machines.\nShift can now begin.");
	}

	private static void setLabourAssignment(SimulationResult row, long endTime) {
		for(LabourAssignment labourAssignment : pmLabourAssignment){
			if(row.t >= labourAssignment.startTime && row.t <= labourAssignment.endTime){
				int labour[] = new int[]{labourAssignment.labour[0]-row.pmLabour[0],
										 labourAssignment.labour[1]-row.pmLabour[1],
										 labourAssignment.labour[2]-row.pmLabour[2]};
				LabourAssignment la  = new LabourAssignment(labour,row.t,endTime);
				pmLabourAssignment.add(la);
				
			}
		}
		int labour[] = new int[]{ pmMaxLabour[0]-row.pmLabour[0],
				 pmMaxLabour[1]-row.pmLabour[1],
				 pmMaxLabour[2]-row.pmLabour[2]};
				 LabourAssignment la  = new LabourAssignment(labour,row.t,endTime);
				 pmLabourAssignment.add(la);
		
		
	}

	private static boolean checkLabourAssignment(SimulationResult row) {
		for(LabourAssignment labourAssignment : pmLabourAssignment){
			if(row.t >= labourAssignment.startTime && row.t <= labourAssignment.startTime){
				for(int i=0;i<3;i++){
					if (row.pmLabour[i] < labourAssignment.labour[i])
						return false;
					}
				return true;
				
			}
		}
		for(int i=0;i<3;i++){
			if (row.pmLabour[i] < pmMaxLabour[i])
				return false;
		}
		return true;
		// TODO Auto-generated method stub
		
	}

	private static long addPMJobs(Schedule schedule,Component[] compList, int compCombo, int pmOpportunity) {
		int cnt = 0;
		long pmTime;
		for(int i=0;i< compList.length;i++){
			int pos = 1<<i;
			if((pos&compCombo)!=0){
				long pmttr = (long)(compList[i].getPMTTR()*Macros.TIME_SCALE_FACTOR);
				//Smallest unit is one hour
				if(pmttr == 0)
					pmttr=1;
				Job pmJob = new Job("PM",pmttr,compList[i].getPMCost(),Job.JOB_PM);
				
				pmJob.setCompCombo(compCombo);
				if(cnt==0){
					pmJob.setFixedCost(compList[i].getPMFixedCost());
				}
				schedule.addPMJob(pmJob,pmOpportunity+cnt);
				pmTime+=pmttr;
				cnt++;
			}
		}
		return pmTime;
	}

}
class CompTTF implements Comparable<CompTTF>{
	public long ttf;
	public int machineID;
	public long ttr;
	public int componentID;
	public CompTTF(long ttf2,long ttr2,int count,int compID) {
		ttf = ttf2;
		machineID = count;
		ttr = ttr2;
		componentID = compID;
	}
	@Override 
	public int compareTo(CompTTF other) {
		return Long.compare(ttf, other.ttf);
	}
	
}

class LabourAssignment{
	int[] labour;
	long startTime;
	long endTime;
	public LabourAssignment(int[] labour,long startTime, long endTime){
		this.labour = labour;
		this.startTime = startTime;
		this.endTime = endTime;
	}
}