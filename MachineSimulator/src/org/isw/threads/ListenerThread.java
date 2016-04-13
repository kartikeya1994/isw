package org.isw.threads;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.BruteForceAlgorithm;
import org.isw.FlagPacket;
import org.isw.IFPacket;
import org.isw.InitConfig;
import org.isw.Job;
import org.isw.Logger;
import org.isw.Machine;
import org.isw.MachineResultPacket;
import org.isw.Macros;
import org.isw.MaintenanceRequestPacket;
import org.isw.MaintenanceTuple;
import org.isw.MemeticAlgorithm;
import org.isw.Schedule;
import org.isw.SimulationResult;

public class ListenerThread extends Thread
{
	Schedule jl;
	InetAddress schedulerIP;
	ServerSocket tcpSocket;
	DatagramSocket udpSocket;
	private InetAddress maintenanceIP =null;
	JobExecutor jobExecutor;
	private double planningTime = 0;
	public ListenerThread(InetAddress schedulerIP, InetAddress maintenanceIP,DatagramSocket udpSocket,ServerSocket tcpSocket) {
		this.schedulerIP = schedulerIP;
		this.tcpSocket = tcpSocket;
		this.udpSocket = udpSocket; 
		this.maintenanceIP = maintenanceIP;	
		jobExecutor = new JobExecutor(udpSocket,tcpSocket, maintenanceIP);
	}

	public void run()
	{
		try {
			tcpSocket.setSoTimeout(0);
			udpSocket.setSoTimeout(0);
			jl = new Schedule(InetAddress.getByName("localhost")); //FIXME: why is this localhost?
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		while(true)
		 {
			try{
				Socket socket = tcpSocket.accept();
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				Object o = in.readObject();
				in.close();
				socket.close();
				if(o instanceof InitConfig){
					init((InitConfig)o);
				}
				else if(o instanceof FlagPacket && ((FlagPacket)o).flag == Macros.PROCESS_COMPLETE){
					// simulation is over
					writeResults(Macros.SIMULATION_COUNT);	
				}
				else if(o instanceof Schedule){
					plan((Schedule)o);		
				}
				else if(o instanceof FlagPacket && ((FlagPacket)o).flag == Macros.REQUEST_PREVIOUS_SHIFT){ // send pending jobs to Scheduling Dept.
					sendPreviousShift();
				}
				
			 } catch(Exception e){
				 e.printStackTrace();
			 }
				}
	}
	void removePMJobs() throws IOException{
		// remove PM jobs that have not been started
				for(int i=0; i<jl.getSize(); i++)
				{
					Job j = jl.jobAt(i);
					if(j.getJobType()==Job.JOB_PM && j.getStatus()==Job.NOT_STARTED)
					{
						jl.remove(i);
						i--;
					}
				}
				
				/*
				 *  if partial PM series is present, set all jobs to NOT_STARTED
				 *  and recalculate seriesTTR and seriesLabour
				 */
				if(!jl.isEmpty() && jl.jobAt(0).getJobType()==Job.JOB_PM)
				{
					long seriesTTR = 0;
					int[] seriesLabour = {0,0,0};
					for(int i=0; i<jl.getSize(); i++)
					{
						// change jobStatus to NOT_STARTED
						Job j = jl.jobAt(i);
						if(j.getJobType()!=Job.JOB_PM)
							break;
						j.setStatus(Job.NOT_STARTED);
						
						// recompute seriesTTR and seriesLabour
						seriesTTR += j.getJobTime();
						if(seriesLabour[0]<j.getSeriesLabour()[0])
							seriesLabour[0] = j.getSeriesLabour()[0];
						if(seriesLabour[1]<j.getSeriesLabour()[1])
							seriesLabour[1] = j.getSeriesLabour()[1];
						if(seriesLabour[2]<j.getSeriesLabour()[2])
							seriesLabour[2] = j.getSeriesLabour()[2];
					}
					
					for(int i=0; i<jl.getSize(); i++)
					{
						Job j = jl.jobAt(i);
						if(j.getJobType()!=Job.JOB_PM)
							break;
						j.setSeriesTTR(seriesTTR);
						j.setSeriesLabour(seriesLabour);
					}
				}
	}
	private void sendPreviousShift() throws IOException {
		removePMJobs();
		
		System.out.println("Sending previous shift to Scheduler");
		jl.send(schedulerIP,Macros.SCHEDULING_DEPT_PORT_TCP);
	}

	private void plan(Schedule schedule) throws InterruptedException, ExecutionException, IOException {
		//Parse schedule from packet.
		jl = schedule;
		Machine.setOldStatus(Machine.getStatus());
		Machine.setStatus(Macros.MACHINE_PLANNING);
		Logger.log(Machine.getStatus(), "Received schedule from scheduler:" + jl.printSchedule()+"\nRunning simulations..");
		System.out.println("Received schedule from scheduler:" + jl.printSchedule());
		System.out.println("Total time" + jl.getSum());
		System.out.println("Planning started");
		long starttime = System.nanoTime();
		/* TODO:
		 * Get simulation results by executing Memetic Algorithm
		 * */
		ExecutorService threadPool = Executors.newSingleThreadExecutor();
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		pool.submit(new SimulationThread(jl,null,null,true,-1));
		SimulationResult result = pool.take().get();
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		ArrayList<Integer> pmos = jl.getPMOpportunities();
		int[] intArray = new int[pmos.size()];
		for (int i = 0; i < intArray.length; i++) {
		    intArray[i] = pmos.get(i);
		}
		SimulationResult[] results = null;
		if(!Macros.NPM){
			if(Macros.BF){
				System.out.println("BF");
				BruteForceAlgorithm bfa = new BruteForceAlgorithm(jl,intArray,result);
				results  = bfa.execute();
				}
			else{
				System.out.println("MA");
				MemeticAlgorithm ma = new MemeticAlgorithm(80,100,jl,intArray,result,false);
				results = ma.execute();
			}
		}
		else{
			System.out.println("NoPM");
		 results = new SimulationResult[0];
		//results = new SimulationResult[1];
		//long combo[]= new long[]{0,0,7,0,0,7,0,0,7,0,0,7};
		//results[0] = new SimulationResult(5,5,combo,intArray,false,1);
		}
		
		
		System.out.println("Sending simulation results to Maintenance");
		
		//Send simulation results to Maintenance Dept.
		IFPacket ifPacket =  new IFPacket(results,jl,Machine.compList);
		ifPacket.send(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP);
		//receive PM incorporated schedule from maintenance
		long endTime = System.nanoTime();
		planningTime  = (endTime - starttime)/Math.pow(10, 9);
		System.out.println("Planning complete in " +(endTime - starttime)/Math.pow(10, 9));
		
		System.exit(0);
		/* This part not required. Evaluation is done centralized, at maintenance dept.
		int count = 0;
		while(count++ < Macros.SIMULATION_COUNT - 1)
		{
			System.out.println("Maintenance planning");
			jl = Schedule.receive(tcpSocket);
			System.out.println("Received socket");
			jobExecutor.execute(jl);
			MaintenanceRequestPacket mrp = new MaintenanceRequestPacket(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP, new MaintenanceTuple(-1));
			mrp.sendTCP();
			
		}
		jl = Schedule.receive(tcpSocket);
		Logger.log(Machine.getStatus(),"Received schedule from maintenance:" + jl.printSchedule());
		System.out.println("Received schedule from maintenance:" + jl.printSchedule());
		System.out.println("Total time" + jl.getSum());
		
		//Execute schedule received by maintenance
		jobExecutor.execute(jl);
		planningTime  = (endTime - starttime)/Math.pow(10, 9);
		System.out.println("Planning complete in " +(endTime - starttime)/Math.pow(10, 9));
		Machine.shiftCount++;
		//Request Scheduling Dept for next shift
		FlagPacket.sendTCP(Macros.REQUEST_NEXT_SHIFT, schedulerIP, Macros.SCHEDULING_DEPT_PORT_TCP);		
	*/
	}

	private void init(InitConfig ic) throws IOException {
		Macros.SHIFT_DURATION = ic.shiftDuration;
		Macros.TIME_SCALE_FACTOR = ic.scaleFactor;
		Macros.SIMULATION_COUNT = ic.simuLationCount;
		Machine.compList = ic.compList;
		System.out.println(Machine.compList.length);
		Machine.compCMJobsDone = new int[Machine.compList.length];
		Machine.compPMJobsDone = new int[Machine.compList.length];
		Logger.connect();
		System.out.println("Machine initialized");
	}

	private void writeResults(int simCount) {
		System.out.println("=========================================");
		double cost = Machine.cmCost + Machine.pmCost + Machine.penaltyCost;
		double downtime = (Machine.cmDownTime + Machine.pmDownTime + Machine.waitTime)/simCount;
		double runtime = 1440 - Machine.idleTime/simCount;
		double availability = 100  - 100*downtime/runtime;
		System.out.format("%f| %f \n",availability,cost/simCount);
		System.out.println("CM Downtime: "+ Machine.cmDownTime/simCount +" hours");
		System.out.println("PM Downtime: "+ Machine.pmDownTime/simCount +" hours");
		System.out.println("Waiting Downtime: "+ Machine.waitTime/simCount +" hours");
		System.out.println("Machine Idle time: "+ Machine.idleTime/simCount+" hours");
		System.out.println("PM Cost: "+ Machine.pmCost/simCount);
		System.out.println("CM Cost: "+ Machine.cmCost/simCount);
		System.out.println("Penalty Cost: "+ Machine.penaltyCost/simCount);
		System.out.println("Processing Cost: "+ Machine.procCost/simCount);
		System.out.println("Jobs processed:" + (double)Machine.jobsDone/simCount);
		System.out.println("Number of CM jobs:" + (double)Machine.cmJobsDone/simCount);
		System.out.println("Number of PM jobs:" + (double)Machine.pmJobsDone/simCount);
		MachineResultPacket mrp = new MachineResultPacket();
		mrp.planningTime = planningTime;
		mrp.availabiltiy = availability;
		mrp.downTime = Machine.downTime/simCount;
		mrp.runTime = Machine.runTime/simCount;
		mrp.cmDownTime = Machine.cmDownTime/simCount;
		mrp.pmDownTime = Machine.pmDownTime/simCount;
		mrp.waitTime = Machine.waitTime/simCount;
		mrp.idleTime = Machine.idleTime/simCount;
		mrp.jobsDone = (double)Machine.jobsDone/simCount;
		mrp.cmJobsDone = (double)Machine.cmJobsDone/simCount;
		mrp.pmJobsDone = (double)Machine.pmJobsDone/simCount;
		mrp.pmCost = Machine.pmCost/simCount;
		mrp.cmCost = Machine.cmCost/simCount;
		mrp.procCost = Machine.procCost/simCount;
		mrp.penaltyCost = Machine.penaltyCost/simCount;
		mrp.compNames = new String[Machine.compList.length];
		for(int i=0;i<mrp.compNames.length;i++)
			mrp.compNames[i] = Machine.compList[i].compName;
		
		mrp.compCMJobsDone = Machine.compCMJobsDone;
		mrp.compPMJobsDone = Machine.compPMJobsDone;
		for(int i=0 ;i<Machine.compList.length; i++){
			System.out.format("Component %s: PM %f| CM %f\n",Machine.compList[i].compName,(double)Machine.compPMJobsDone[i]/simCount,(double)Machine.compCMJobsDone[i]/simCount);
		}
		Logger.log(mrp);
	}
	
}