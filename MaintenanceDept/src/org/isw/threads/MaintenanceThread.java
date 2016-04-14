

package org.isw.threads;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.isw.LabourAvailability;
import org.isw.Machine;
import org.isw.MachineList;
import org.isw.Macros;
import org.isw.Maintenance;
import org.isw.Schedule;
import org.isw.SimulationResult;

public class MaintenanceThread  extends Thread{
	MachineList machineList;
	static ServerSocket tcpSocket;
	static ArrayList<SimulationResult> table = new ArrayList<SimulationResult>();
	static ArrayList<InetAddress> ip = new ArrayList<InetAddress>();
	static ArrayList<Integer> port = new ArrayList<Integer>();
	static ArrayList<Schedule> schedule = new ArrayList<Schedule>();
	static ArrayList<Component[]> component = new ArrayList<Component[]>();
	static ArrayList<double[]> pmTTRList = new ArrayList<double[]>();
	static int numOfMachines;
	static PriorityQueue<CompTTF> ttfList;
	LabourAvailability pmLabourAssignment;
	private ArrayList<Machine> machines;
	
	public MaintenanceThread(MachineList machineList){
		this.machineList = machineList;
		
		try {
			tcpSocket = new ServerSocket(Macros.MAINTENANCE_DEPT_PORT_TCP);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run()
	{
		//while(true)
			startShift();
	}
	public void startShift()
	{
		System.out.format("Max Labour: %d %d %d\n",Maintenance.maxLabour[0],Maintenance.maxLabour[1],Maintenance.maxLabour[2]);
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
			machines = new ArrayList<Machine>();
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
				machines.add(new Machine(i,p.compList));
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
		
		// labour availability during planning
		pmLabourAssignment = new LabourAvailability(Maintenance.maxLabour.clone(), Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR);
		
		// reserve labour for jobs pending from previous shift
		for(int i=0; i<schedule.size(); i++)
		{
			Schedule sched = schedule.get(i);
			if(!sched.isEmpty() && sched.jobAt(0).getJobType()==Job.JOB_PM)
			{
				//pending PM job present in this schedule
				System.out.println("Reserving Labour for previous shift PM job");
				//reserve labour for it
				pmLabourAssignment.employLabour(0, sched.jobAt(0).getSeriesTTR(), sched.jobAt(0).getSeriesLabour());
			}
			/*
			if(!sched.isEmpty() && sched.jobAt(0).getJobType()==Job.JOB_CM)
			{
				//pending PM job present in this schedule
				
				//reserve labour for it
				pmLabourAssignment.employLabour(0, sched.jobAt(0).getSeriesTTR(), sched.jobAt(0).getSeriesLabour());
			}*/
			
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
					System.out.println("Complist"+compList.length);
					for(int compno=0;compno<compList.length;compno++)
					{
						System.out.println("Combo:"+row.compCombo[pmOpp]);
						int pos = 1<<compno;
						if((pos&row.compCombo[pmOpp])!=0) //for each component in combo, generate TTR
						{
							row.pmTTRs[pmOpp][compno] = Component.notZero(compList[compno].getPMTTR()*Macros.TIME_SCALE_FACTOR); //store PM TTR
							System.out.println("pmTTR: "+row.pmTTRs[pmOpp][compno]);
							seriesTTR[pmOpp] += row.pmTTRs[pmOpp][compno];
							
							// find max labour requirement for PM series
							int[] labour1 = compList[compno].getPMLabour();
							
							if(seriesLabour[pmOpp][0] < labour1[0])
								seriesLabour[pmOpp][0] = labour1[0];
							if(seriesLabour[pmOpp][1] < labour1[1])
								seriesLabour[pmOpp][1] = labour1[1];
							if(seriesLabour[pmOpp][2] < labour1[2])
								seriesLabour[pmOpp][2] = labour1[2];
							System.out.format("Series Labour: %d %d %d\n", seriesLabour[pmOpp][0],seriesLabour[pmOpp][1],seriesLabour[pmOpp][2]);
							//pmLabourAssignment.print();
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
					System.out.println("Met requirements for all pmOpp");
					//incorporate the PM job(s) into schedule of machine
					
					
					addPMJobs(schedule.get(row.id), component.get(row.id), row, seriesTTR, seriesLabour);
					long[] chromosome = row.compCombo;
					String str = schedule.get(row.id).printSchedule();
					try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("final_schedule_"+schedule.size(),true)))){ 
						out.print("Combo:" );
						for(int k=0;k<component.get(row.id).length;k++)
							out.format("%s,",component.get(row.id)[k].compName);
						for(int k=0;k<chromosome.length;k++)
							out.print(String.format("%3s |", Long.toBinaryString(chromosome[k]).replace(" ","0")));
						out.println();	
						out.println(str);
						out.println();
						out.println();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					toPerformPM.put(row.id, true);
					
					//reserve labour
					for(int pmOpp = 0; pmOpp<row.pmOpportunity.length; pmOpp++)
						pmLabourAssignment.employLabour(row.startTimes[pmOpp], row.startTimes[pmOpp]+seriesTTR[pmOpp], seriesLabour[pmOpp]);
				}
			}
		}
		
		System.out.println("PM incorporated schedule- ");
		for(int i=0; i<numOfMachines;i++)
			System.out.println("Machine: "+ip.get(i)+"\nSchedule: "+schedule.get(i).printSchedule());
		
		ExecutorService threadPool = Executors.newFixedThreadPool(1);
		for(int i=0;i<Macros.SIMULATION_COUNT;i++)
			threadPool.execute(new ScheduleExecutionThread(schedule,machines));
		threadPool.shutdown();
		while(!threadPool.isTerminated()); 
		
		
		for(Machine machine : machines)
			writeResults(machine,Macros.SIMULATION_COUNT);
		
		//sending PM incorporated schedule to respective machines
		/*System.out.println("Sending to all machines...");
		int count = 0;
		while(count++ < Macros.SIMULATION_COUNT){
			
			System.out.println("Maintenance planning");
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
			LabourAvailability realTimeLabour = new LabourAvailability(Maintenance.maxLabour.clone(), Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR);
			int[] currentLabour = Maintenance.maxLabour.clone();
	
			Logger.log(currentLabour, "Shift started: "+count);
			int mcnt = 0;
			while(true)
			{
				MaintenanceRequestPacket packet = MaintenanceRequestPacket.receiveTCP(tcpSocket, 0);
			
				if(packet.mtTuple.start == -1) // packet sent by Scheduling Dept indicating shift is over
				{
					if(count < Macros.SIMULATION_COUNT){
						System.out.println("Count: "+count);
						if(mcnt++ == numOfMachines-1){
							break;	
						}
					}
					// shift is over
					else {
						System.out.println("End");
						break;
					}
				}
			
				else if(packet.mtTuple.start == -2) 
				{
					// some machine is reporting PM job completion
				currentLabour[0]+=packet.mtTuple.labour[0];
				currentLabour[1]+=packet.mtTuple.labour[1];
				currentLabour[2]+=packet.mtTuple.labour[2];
				//Logger.log(currentLabour, "Maintenance job over at "+packet.machineIP.getHostAddress());
			}
			
			else
			{
				if(realTimeLabour.checkAvailability(packet.mtTuple))
				{
					
					//labour is available. Grant request and reserve labour
					realTimeLabour.employLabour(packet.mtTuple);
					//realTimeLabour.print();
					FlagPacket.sendTCP(Macros.LABOUR_GRANTED, packet.machineIP, Macros.MACHINE_PORT_TCP);
					
					// log a decrease in available labour
					currentLabour[0]-=packet.mtTuple.labour[0];
					currentLabour[1]-=packet.mtTuple.labour[1];
					currentLabour[2]-=packet.mtTuple.labour[2];
					//Logger.log(currentLabour, "Maintenance job started at " +packet.machineIP.getHostAddress());
				}
				else
				{
					
					//realTimeLabour.print();
					System.out.println("Denied");
					//deny request
					FlagPacket.sendTCP(Macros.LABOUR_DENIED, packet.machineIP,Macros.MACHINE_PORT_TCP);
				}
				}
			}
		}
		System.out.println("Printing final schedule: ");
		for(int i=0;i<schedule.size();i++)
			System.out.println(i+':'+schedule.get(i).printSchedule());*/
	}

	private static void writeResults(Machine machine, int simCount) {
		System.out.println("Version 2.0.4");
		double cost = machine.cmCost + machine.pmCost + machine.penaltyCost;
		double downtime = (machine.cmDownTime + machine.pmDownTime + machine.waitTime)/simCount;
		double runtime = 1440 - machine.idleTime/simCount;
		double availability = 100  - 100*downtime/runtime;
		System.out.println("=========================================");
		System.out.println("Machine "+ (machine.machineNo+1));
		System.out.format("%f| %f \n",availability,cost/simCount);
		//System.out.println("Downtime:" + String.valueOf(machine.downTime*100/(machine.runTime)) +"%");
		System.out.println("CMDowntime: "+ machine.cmDownTime/simCount +" hours");
		System.out.println("PM Downtime: "+ machine.pmDownTime/simCount +" hours");
		System.out.println("Waiting Downtime: "+ machine.waitTime/simCount +" hours");
		System.out.println("Machine Idle time: "+ machine.idleTime/simCount+" hours");
		System.out.println("PM Cost: "+ machine.pmCost/simCount);
		System.out.println("CM Cost: "+ machine.cmCost/simCount);
		System.out.println("Penalty Cost: "+ machine.penaltyCost/simCount);
		System.out.println("Processing Cost: "+ machine.procCost/simCount);
		System.out.println("Number of jobs:" + (double)machine.jobsDone/simCount);
		System.out.println("Number of CM jobs:" + (double)machine.cmJobsDone/simCount);
		System.out.println("Number of PM jobs:" + (double)machine.pmJobsDone/simCount);
		for(int i=0 ;i<machine.compList.length; i++)
			System.out.println("Component "+machine.compList[i].compName+": PM "+(double)machine.compPMJobsDone[i]/simCount+"|CM "+(double)machine.compCMJobsDone[i]/simCount);
		
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("results.csv",true)))){ 
			for(int i=0;i<machine.compList.length;i++)
				out.format("%s;", machine.compList[i].compName);
			out.print(",");
			out.print("0,");
			out.format("%f,", availability);
			out.format("%d,",  machine.idleTime/simCount);
			out.format("%d,", machine.pmDownTime/simCount);
			out.format("%d,",  machine.cmDownTime/simCount);
			out.format("%d,", machine.waitTime/simCount);
			out.format("%f,",  machine.pmCost/simCount);
			out.format("%f,", machine.cmCost/simCount);
			out.format("%d,",  machine.penaltyCost/simCount);
			out.format("%f,", cost/simCount);
			out.format("%d,",  machine.procCost/simCount);
			out.format("%f,",cost/simCount+machine.procCost/simCount);
			out.format("%f,",  (double)machine.jobsDone/simCount);
			out.format("%f,", (double)machine.pmJobsDone/simCount);
			out.format("%f\n",  (double)machine.cmJobsDone/simCount);
		
		}
		catch(IOException e){

		}
	
	}
	private static void addPMJobs(Schedule schedule,Component[] compList, SimulationResult row, long[] seriesTTR, int[][] seriesLabour) {
		/*
		 * Add PM jobs to given schedule.
		 */
		int cnt = 0;
		int[] pmOpportunity = row.pmOpportunity;
		long[] compCombo = row.compCombo;
		
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
					pmJob.setSeriesTTR(seriesTTR[pmOpp]);
					pmJob.setSeriesLabour(seriesLabour[pmOpp]);
					if(cnt==0){
						// consider fixed cost only once, for the first job
						pmJob.setFixedCost(compList[i].getPMFixedCost());
					}
					
					// add job to schedule
					schedule.addPMJob(new Job(pmJob),pmOpportunity[pmOpp]+cnt);

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