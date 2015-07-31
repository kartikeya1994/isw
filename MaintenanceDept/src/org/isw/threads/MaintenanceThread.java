
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
import org.isw.FlagPacket;
import org.isw.IFPacket;
import org.isw.Job;
import org.isw.LabourAvailability;
import org.isw.MachineList;
import org.isw.Macros;
import org.isw.MaintenanceRequestPacket;
import org.isw.MaintenanceTuple;
import org.isw.Schedule;
import org.isw.SimulationResult;

public class MaintenanceThread  extends Thread{
	MachineList machineList;
	static DatagramSocket socket;
	static ServerSocket tcpSocket;
	static int[] maxLabour;
	static ArrayList<SimulationResult> table = new ArrayList<SimulationResult>();
	static ArrayList<InetAddress> ip = new ArrayList<InetAddress>();
	static ArrayList<Integer> port = new ArrayList<Integer>();
	static ArrayList<Schedule> schedule = new ArrayList<Schedule>();
	static ArrayList<Component[]> component = new ArrayList<Component[]>();
	static ArrayList<double[]> pmTTRList = new ArrayList<double[]>();
	static int numOfMachines;
	static PriorityQueue<CompTTF> ttfList;
	static LabourAvailability pmLabourAssignment = new LabourAvailability(maxLabour, Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR);
	
	public MaintenanceThread(MachineList machineList){
		this.machineList = machineList;
		
		try {
			socket = new DatagramSocket(Macros.MAINTENANCE_DEPT_PORT);
			tcpSocket = new ServerSocket(Macros.MAINTENANCE_DEPT_PORT_TCP);
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
				//get intensity factors and schedules from all machines in machine list
				numOfMachines++;
				pool.submit(new FetchIFTask(tcpSocket, ips.nextElement(), Macros.MACHINE_PORT_TCP));
			}

			
			System.out.println("Fetching IFs and schedules from " + numOfMachines + " connected machines...");
			
			table = new ArrayList<SimulationResult>(); // consolidated table of IFs 
			ip = new ArrayList<InetAddress>();
			port = new ArrayList<Integer>();
			schedule = new ArrayList<Schedule>();
			component = new ArrayList<Component[]>();
			ttfList = new PriorityQueue<CompTTF>();

			for(int i = 0; i < numOfMachines; i++)
			{
				//fetch results from thread pool
				IFPacket p = pool.take().get(); 
				System.out.println("Machine " + p.ip + "\n" + p);
				ip.add(p.ip);
				port.add(p.port);
				Schedule sched = p.jobList;
				schedule.add(sched);
				component.add(p.compList);
				pmTTRList.add(new double[p.compList.length]);
				
				for(int j=0;j<p.results.length;j++)
				{
					p.results[j].id = i; //assign machine id to uniquely identify machine
					for(int pmOpp=0; pmOpp < p.results[j].pmOpportunity.length; pmOpp++)
					{
						// calculate start times for each job in SimulationResult
						if(p.results[j].pmOpportunity[pmOpp] <= 0){
							p.results[j].startTimes[pmOpp] = 0; //assign calculated t
						}
						else{
							// start time of PM job is finishing time of job before it
							p.results[j].startTimes[pmOpp] = sched.getFinishingTime(p.results[j].pmOpportunity[pmOpp]-1);
						}
					}
					if(!p.results[j].noPM){
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
		 
		//sort
		Collections.sort(table, new CustomComparator(component)); //higher IF and lower labour requirement

		HashMap<Integer, Boolean> toPerformPM = new HashMap<Integer,Boolean>(); //only one PM per machine per shift.
		
		for(int i=0;i<table.size();i++)
		{
			SimulationResult row = table.get(i);
			Component[] compList = component.get(row.id);
			row.pmTTRs = new long[row.pmOpportunity.length][compList.length];
			// check if PM is already being planned for machine (only 1 PM per shift)
			// or if schedule is empty
			if(!toPerformPM.containsKey(row.id) && !schedule.get(row.id).isEmpty())
			{
				// generate PM TTRs of all components undergoing PM for this row
				boolean meetsReqForAllOpp = true;
				int[][] seriesLabour = new int[row.pmOpportunity.length][3];
				long[] seriesTTR = new long[row.pmOpportunity.length];
				for(int pmOpp = 0; pmOpp<row.pmOpportunity.length; pmOpp++)
				{					
					seriesLabour[pmOpp][0] = 0;
					seriesLabour[pmOpp][1] = 0;
					seriesLabour[pmOpp][2] = 0;
					seriesTTR[pmOpp] = 0;
					
					for(int compno=0;i< compList.length;i++)
					{
						int pos = 1<<compno;
						if((pos&row.compCombo[pmOpp])!=0) //for each component in combo, generate TTR
						{
							row.pmTTRs[pmOpp][compno] = Component.notZero(compList[compno].getPMTTR()*Macros.TIME_SCALE_FACTOR); //store PM TTR
							
							seriesTTR[pmOpp] += row.pmTTRs[pmOpp][compno];
							
							// find max labour requirement for PM series
							int[] labour1 = compList[compno].getPMLabour();
							if(seriesLabour[pmOpp][0] < labour1[0])
								seriesLabour[pmOpp][0] = labour1[0];
							if(seriesLabour[pmOpp][1] < labour1[1])
								seriesLabour[pmOpp][1] = labour1[1];
							if(seriesLabour[pmOpp][2] < labour1[2])
								seriesLabour[pmOpp][2] = labour1[2];
						}
					}
					if(!pmLabourAssignment.checkAvailability(row.startTimes[pmOpp], row.startTimes[pmOpp]+seriesTTR[pmOpp], seriesLabour[pmOpp]))
					{
						// add series of PM jobs at that opportunity.
						meetsReqForAllOpp = false;
						break;
					}
				}
				
				if(meetsReqForAllOpp)
				{
					//incorporate the PM job(s) into schedule of machine
					addPMJobs(schedule.get(row.id), component.get(row.id),row);
					toPerformPM.put(row.id, true);
					
					//reserve labour
					for(int pmOpp = 0; pmOpp<row.pmOpportunity.length; pmOpp++)
						pmLabourAssignment.employLabour(row.startTimes[pmOpp], row.startTimes[pmOpp]+seriesTTR[pmOpp], seriesLabour[pmOpp]);
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
		
		// shift has begun
		// receive and process requests for labour
		
		LabourAvailability labourAvailability = new LabourAvailability(maxLabour, Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR);
		
		while(true)
		{
			MaintenanceRequestPacket packet = MaintenanceRequestPacket.receiveTCP(tcpSocket, 0);
			
			if(packet.mtTuple.start == -1) // packet sent by Scheduling Dept indicating shift is over
			{
				// shift is over
				break;
			}
			
			else
			{
				if(labourAvailability.checkAvailability(packet.mtTuple))
				{
					//labour is available. Grant request and reserve labour
					labourAvailability.employLabour(packet.mtTuple);
					FlagPacket.sendTCP(Macros.LABOUR_GRANTED, packet.machineIP, packet.machinePort);
					
				}
				else
				{
					//deny request
					FlagPacket.sendTCP(Macros.LABOUR_DENIED, packet.machineIP, packet.machinePort);
				}
			}
		}
	}

	private static void addPMJobs(Schedule schedule,Component[] compList, SimulationResult row) {
		/*
		 * Add PM jobs to given schedule.
		 */
		int cnt = 0;
		int[] pmOpportunity = row.pmOpportunity;
		int[] compCombo = row.compCombo;
		
		for(int pmOpp = 0; pmOpp<pmOpportunity.length; pmOpp++)
		{
			for(int i=0;i< compList.length;i++)
			{
				int pos = 1<<i;
				if((pos&compCombo[pmOpp])!=0) //for each component in combo, add a PM job
				{
					long pmttr = Component.notZero(row.pmTTRs[pmOpp][i]);
					
					Job pmJob = new Job("PM",pmttr,compList[i].getPMLabourCost(),Job.JOB_PM);
					pmJob.setCompNo(i);
					if(cnt==0){
						// consider fixed cost only once, for the first job
						pmJob.setFixedCost(compList[i].getPMFixedCost());
					}
					
					// add job to schedule
					schedule.addPMJob(pmJob,pmOpportunity[pmOpp]+cnt);

					cnt++;
				}
			}
		}
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