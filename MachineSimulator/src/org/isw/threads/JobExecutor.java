package org.isw.threads;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.BruteForceAlgorithm;
import org.isw.Component;
import org.isw.FlagPacket;
import org.isw.IFPacket;
import org.isw.Job;
import org.isw.MachineLogger;
import org.isw.Machine;
import org.isw.MachineResultPacket;
import org.isw.Macros;
import org.isw.MaintenanceRequestPacket;
import org.isw.MaintenanceTuple;
import org.isw.MemeticAlgorithm;
import org.isw.Schedule;
import org.isw.SimulationResult;

public class JobExecutor{
	DatagramSocket socket;
	DatagramPacket timePacket, replanPacket, idlePacket;
	ServerSocket tcpSocket;
	Component[] compList;
	InetAddress maintenanceIP, schedulerIP;
	Schedule jobList;
	LinkedList<FailureEvent> failureEvents;
	FailureEvent upcomingFailure;
	boolean replan;
	long time;
	public JobExecutor(DatagramSocket socket, ServerSocket tcpSocket,InetAddress schedulerIP, InetAddress maintenanceIP){
		this.socket = socket;
		this.tcpSocket = tcpSocket;
		this.maintenanceIP = maintenanceIP;
		this.schedulerIP = schedulerIP;
		timePacket = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.REQUEST_TIME);
		replanPacket = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.REPLAN);
		idlePacket = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.JOBS_DONE);
		time = 0;
		replan = false;
	}

	public void timeSync() throws IOException //end of one time unit
	{
		//send appropriate timeSync packet
		//System.out.println("Time: "+time);
		MachineLogger.timeLog(time);
		if(replan)
		{
			//System.out.println("Sending replan packet time: "+ time);
			//socket.send(replanPacket);
			FlagPacket.sendTCP(Macros.REPLAN, schedulerIP, Macros.SCHEDULING_DEPT_PORT_TCP_TIMESYNC);
		}
		else
			//socket.send(timePacket);
			FlagPacket.sendTCP(Macros.REQUEST_TIME, schedulerIP, Macros.SCHEDULING_DEPT_PORT_TCP_TIMESYNC);
			
		//get timesync response
		//FlagPacket res = FlagPacket.receiveUDP(socket);
		FlagPacket res = FlagPacket.receiveTCP(tcpSocket,0);
		
//		if(replan)
//			if(res.flag == Macros.INITIATE_REPLAN)
//				System.out.println("Replan start packet received, time: "+ time);

		if(res.flag == Macros.INITIATE_REPLAN)
		{
			try{
				int oldStatus = Machine.getStatus();
				Machine.setStatus(Macros.MACHINE_REPLANNING);
				MachineLogger.log(Machine.getStatus(), "Replanning");
				System.out.println("Replanning, time: "+time);

				FlagPacket.sendTCP(Macros.SCHED_REPLAN_INIT, schedulerIP, Macros.SCHEDULING_DEPT_PORT_TCP);

				sendRemainingJobs();

				getNewJobs();

				planPM();

				Machine.setStatus(oldStatus);
				MachineLogger.log(Machine.getStatus(), "Replanning finished.");
				System.out.println("Replanning finished.");
				replan = false;
			}catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	void getNewJobs() throws Exception
	{
		try{
			Socket socket = tcpSocket.accept();
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
			Object o = in.readObject();
			in.close();
			socket.close();
			jobList = (Schedule) o;
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	void planPM() throws InterruptedException, ExecutionException, NumberFormatException, IOException
	{
		System.out.println("Planning started");
		long starttime = System.nanoTime();
		//get NoPM Cost
		ExecutorService threadPool = Executors.newSingleThreadExecutor();
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		pool.submit(new SimulationThread(jobList,null,null,true,-1));
		SimulationResult noPMSim = pool.take().get();
		threadPool.shutdown();
		while(!threadPool.isTerminated());

		ArrayList<Integer> pmos = jobList.getPMOpportunities();
		int[] intArray = new int[pmos.size()];
		for (int i = 0; i < intArray.length; i++) {
			intArray[i] = pmos.get(i);
		}
		SimulationResult[] results = null;

		long problemSize = jobList.getSize()*compList.length;

		if(problemSize<12)
		{
			/*
			 * Use Brute Force Search for smaller problem sizes
			 * problemSize = no. of components * no. of PM opportunities
			 * 2^(problemSize) gives all possible PM schedules.
			 */
			System.out.println("Using: Brute Force Search");
			BruteForceAlgorithm bfa = new BruteForceAlgorithm(jobList,intArray,noPMSim);
			results  = bfa.execute();
		}
		else
		{
			System.out.println("Using: Memetic Algorithm");
			//TODO:
			MemeticAlgorithm ma = new MemeticAlgorithm(500,200,jobList,intArray,noPMSim,false);
			results = ma.execute();
		}

		System.out.println("Sending simulation results to Maintenance");

		//Send simulation results to Maintenance Dept.
		IFPacket ifPacket =  new IFPacket(results,jobList,Machine.compList);
		ifPacket.send(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP);
		//receive PM incorporated schedule from maintenance

		long endTime = System.nanoTime();
		System.out.println("Planning complete in " +(endTime - starttime)/Math.pow(10, 9));

		//receive PM incp schedule from Maintenance
		jobList = Schedule.receive(tcpSocket);
		System.out.println("Received PM incorporated schedule from Maintenance.");
		MachineLogger.log(Machine.getStatus(), "New schedule: "+jobList.printSchedule());
		System.out.println("New schedule: "+jobList.printSchedule());
	}

	void removePMJobs(Schedule jl) throws IOException{
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
			//int[] seriesLabour = {0,0,0};
			for(int i=0; i<jl.getSize(); i++)
			{
				// change jobStatus to NOT_STARTED
				Job j = jl.jobAt(i);
				if(j.getJobType()!=Job.JOB_PM)
					break;
				j.setStatus(Job.NOT_STARTED);

				// recompute seriesTTR and seriesLabour
				seriesTTR += j.getJobTime();
//				if(seriesLabour[0]<j.getSeriesLabour()[0])
//					seriesLabour[0] = j.getSeriesLabour()[0];
//				if(seriesLabour[1]<j.getSeriesLabour()[1])
//					seriesLabour[1] = j.getSeriesLabour()[1];
//				if(seriesLabour[2]<j.getSeriesLabour()[2])
//					seriesLabour[2] = j.getSeriesLabour()[2];
			}

			for(int i=0; i<jl.getSize(); i++)
			{
				Job j = jl.jobAt(i);
				if(j.getJobType()!=Job.JOB_PM)
					break;
				j.setSeriesTTR(seriesTTR);
				//j.setSeriesLabour(seriesLabour);
			}
		}
	}

	private void sendRemainingJobs() throws IOException, ClassNotFoundException 
	{
		//wait for request
		Socket socket = tcpSocket.accept();
		ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
		in.readObject();
		in.close();
		socket.close();

		removePMJobs(jobList);

		//send
		System.out.println("REPLAN: Sending pending jobs to Scheduler");
		jobList.send(schedulerIP,Macros.SCHEDULING_DEPT_PORT_TCP);
	}

	private void computeFailureEvents()
	{
		failureEvents = new LinkedList<FailureEvent>();
		upcomingFailure = null;
		for(int compNo=0; compNo< compList.length; compNo++)
		{
			long ft = time + (long) compList[compNo].getCMTTF()*Macros.TIME_SCALE_FACTOR;
			failureEvents.add(new FailureEvent(compNo, ft));
		}

		if(!failureEvents.isEmpty())
		{
			Collections.sort(failureEvents, new FailureEventComparator());
			upcomingFailure =  failureEvents.pop();
		}
	}

	public void execute(Schedule schedule) throws IOException
	{
		compList = new Component[Machine.compList.length];
		for(int i=0;i< Machine.compList.length;i++)
			compList[i] = new Component(Machine.compList[i]);
		jobList = new Schedule(schedule);
		Machine.setStatus(Machine.getOldStatus());

		// find all machine failures and CM times for this shift
		computeFailureEvents();

		// run machine
		System.out.println("\n***********\nStarting shift\n***********\n");
		while(true)
		{
			if(time == Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR)
			{
				System.out.println("SHIFT OVER");
				time = 0;
				if(!jobList.isEmpty()){
					int i = jobList.indexOf(jobList.peek());
					while(i < jobList.getSize()){
						Machine.penaltyCost += jobList.jobAt(i++).getPenaltyCost()*jobList.jobAt(i-1).getJobTime();
					}
				}
				return;
			}

			if(jobList.isEmpty())
			{
				Machine.idleTime++;
				time++;
				timeSync();
				continue;
			}

			Job current = jobList.peek(); //get current job
			if(time>upcomingFailure.failureTime) // get upcoming failure
			{
				if(!failureEvents.isEmpty())
					upcomingFailure = failureEvents.pop();
			}

			/*
			 * Perform action according to what job is running
			 * Increment costs or wait for labour to arrive for CM/PM
			 */
			boolean failureEvent = false;
			synchronized(Machine.lock){
				failureEvent = Machine.failureEvent;
				if(failureEvent)
					Machine.failureEvent = false;
			}
			if(current.getJobType()!= Job.JOB_CM && current.getJobType()!= Job.JOB_PM 
					&& (failureEvent ||upcomingFailure!=null && time == upcomingFailure.failureTime))
			{
				/*
				 * Machine fails. 
				 * Add CM job to top of schedule.
				 * Set replan to true
				 */
				System.out.println("\n**************\nMachine Failed\n**************");
				MachineLogger.log(Machine.getStatus(), "Machine Failed. Requesting maintenance...");
				Job cmJob = new Job("CM", upcomingFailure.repairTime, Machine.compList[upcomingFailure.compNo].getCMLabourCost(), Job.JOB_CM);
				cmJob.setFixedCost(Machine.compList[upcomingFailure.compNo].getCMFixedCost());
				cmJob.setCompNo(upcomingFailure.compNo);
				jobList.addJobTop(cmJob);

				Machine.setStatus(Macros.MACHINE_WAITING_FOR_CM_LABOUR);
				MachineLogger.log(Machine.getStatus(), "");
				current = jobList.peek();

				replan = true;
				//time++;
				System.out.println("Initiating replan. time: "+time);
				//timeSync();
				//continue;
			}

			if(!replan && (Machine.getStatus() == Macros.MACHINE_WAITING_FOR_CM_LABOUR 
					|| Machine.getStatus() == Macros.MACHINE_WAITING_FOR_PM_LABOUR))
			{
				System.out.println("Waiting for labour ");
				// see if maintenance labour is available at this time instant
				int[] labour_req = null;

				// determine labour requirement
				if(current.getJobType() == Job.JOB_CM)
					labour_req = compList[current.getCompNo()].getCMLabour();
				else if(current.getJobType() == Job.JOB_PM)
					labour_req = compList[current.getCompNo()].getPMLabour();

				// send labour request
				MaintenanceTuple mtTuple;
				if(Machine.getStatus() == Macros.MACHINE_WAITING_FOR_CM_LABOUR)
					mtTuple = new MaintenanceTuple(time, time+current.getJobTime(), labour_req);
				else
				{
					mtTuple = new MaintenanceTuple(time, time+current.getSeriesTTR(), current.getSeriesLabour());
				}

				System.out.format("Labour req: %d %d %d\n", mtTuple.labour[0],mtTuple.labour[1],mtTuple.labour[2]);

				MaintenanceRequestPacket mrp = new MaintenanceRequestPacket(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP, mtTuple);
				mrp.sendTCP();


				FlagPacket flagPacket = FlagPacket.receiveTCP(tcpSocket, 0);
				if(flagPacket.flag == Macros.LABOUR_GRANTED)
				{
					System.out.println("Request granted");
					// labour is available, perform maintenance job
					if(current.getJobType() == Job.JOB_CM)
						Machine.setStatus(Macros.MACHINE_CM);
					if(current.getJobType() == Job.JOB_PM)
						Machine.setStatus(Macros.MACHINE_PM);
					MachineLogger.log(Machine.getStatus(), "Request granted");
					continue;
				}
				else if(flagPacket.flag == Macros.LABOUR_DENIED)
				{
					System.out.println("Request denied. Not enough labour");

					MachineLogger.log(Machine.getStatus(),"Request denied. Not enough labour");
					// machine waits for labour
					// increment cost models accordingly
					Machine.downTime++;
					Machine.waitTime++;
				}
				else
					System.out.println("\n\n**************\nERROR WHILE REQUESTING MAINTENANCE FOR STATUS\n**************\n");
			}

			else if(current.getJobType() == Job.JOB_NORMAL)
			{
				if(Machine.getStatus()!= Macros.MACHINE_RUNNING_JOB)
				{
					Machine.setStatus(Macros.MACHINE_RUNNING_JOB);
					if(current.getStatus() == Job.STARTED)
					{
						System.out.println("Resuming Job "+ current.getJobName());
						MachineLogger.log(Machine.getStatus(), "Resuming Job "+ current.getJobName());
					}
				}
				
				if(current.getStatus() != Job.STARTED)
				{
					current.setStatus(Job.STARTED);
					System.out.format("Job %s started.\n",current.getJobName());
					MachineLogger.log(Machine.getStatus(), "Job "+current.getJobName()+" started.");
				}
				
				// no failure, no maintenance. Just increment cost models normally.
				Machine.procCost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				for(Component comp : compList)
					comp.initAge++;
				Machine.runTime++;
			}

			else if(current.getJobType() == Job.JOB_PM)
			{
				if(Machine.getStatus() != Macros.MACHINE_PM)
				{
					// request PM if labours not yet allocated
					System.out.println("Waiting for PM labour");
					Machine.setStatus(Macros.MACHINE_WAITING_FOR_PM_LABOUR);
					MachineLogger.log(Machine.getStatus(), "");
					continue;
				}

				// since an actual PM job is a series of PM jobs of each comp in compCombo
				// we set all jobs in series to SERIES_STARED
				if(current.getStatus() == Job.NOT_STARTED)
				{
					System.out.println("PM Started");
					current.setStatus(Job.STARTED);
					for(int i=1; i<jobList.getSize(); i++)
					{
						Job j = jobList.jobAt(i);
						if(j.getJobType() != Job.JOB_PM)
							break;
						j.setStatus(Job.SERIES_STARTED);
					}
				}
				else if(current.getStatus() == Job.SERIES_STARTED)
					current.setStatus(Job.STARTED);

				Machine.pmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				Machine.pmDownTime++;
				Machine.downTime++;				
			}
			else if(current.getJobType() == Job.JOB_CM && Machine.getStatus() == Macros.MACHINE_CM)
			{
				if(current.getStatus() != Job.STARTED)
				{
					System.out.println("CM started.");
					current.setStatus(Job.STARTED);
					MachineLogger.log(Machine.getStatus(), "CM started.");
				}
				Machine.cmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				Machine.downTime++;
				Machine.cmDownTime++;
			}

			// decrement job time by unit time
			try{
				if(Machine.getStatus()==Macros.MACHINE_RUNNING_JOB || Machine.getStatus()==Macros.MACHINE_CM || Machine.getStatus()==Macros.MACHINE_PM)
				{
					jobList.decrement(1);
				}
			}
			catch(IOException e){
				e.printStackTrace();
				System.exit(0);
			}
			time++;
			// if job has completed remove job from schedule
			if(current.getJobTime()<=0)
			{
				switch(current.getJobType())
				{
				case Job.JOB_PM:
					Component comp1 = compList[current.getCompNo()];
					comp1.initAge = (1-comp1.pmRF)*comp1.initAge;
					Machine.compPMJobsDone[current.getCompNo()]++;
					Machine.pmJobsDone++;

					if(jobList.getSize()<=1 || jobList.jobAt(1).getJobType() != Job.JOB_PM)
					{
						MaintenanceTuple release = new MaintenanceTuple(-2, 0, current.getSeriesLabour());
						MaintenanceRequestPacket mrp = new MaintenanceRequestPacket(maintenanceIP, 
																			Macros.MAINTENANCE_DEPT_PORT_TCP, release);
						mrp.sendTCP();
					}

					// recompute component failures
					computeFailureEvents();

					break;

				case Job.JOB_CM:
					// 
					Component comp = compList[current.getCompNo()];
					comp.initAge = (1 - comp.cmRF)*comp.initAge;
					Machine.cmJobsDone++;
					Machine.compCMJobsDone[current.getCompNo()]++;

					MaintenanceTuple release = new MaintenanceTuple(-2, 0, comp.getCMLabour());
					MaintenanceRequestPacket mrp = new MaintenanceRequestPacket(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP, release);
					mrp.sendTCP();

					// recompute component failures
					computeFailureEvents();

					break;

				case Job.JOB_NORMAL:
					Machine.jobsDone++;
					break;
				}
				try{
					Job job = jobList.remove();
					// job is complete, remove from joblist
					System.out.println("Job "+ job.getJobName()+" complete");
					// update Machine status on job completion
					if(jobList.isEmpty())
						Machine.setStatus(Macros.MACHINE_IDLE);
					MachineLogger.log(Machine.getStatus(), "Job "+ job.getJobName()+" complete");
				}
				catch(IOException e){
					e.printStackTrace();
					System.exit(0);
				}
			}		

			//Poll for synchronization		
			timeSync();
		}
	}

	public static void writeResults() 
	{
		int simCount =1;
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
		mrp.planningTime = Machine.planningTime;
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
		MachineLogger.log(mrp);
	}


	class FailureEvent
	{
		public int compNo;
		public long repairTime;
		public long failureTime;

		public FailureEvent(int compNo, long failureTime)
		{
			this.compNo = compNo;
			this.repairTime = Component.notZero(compList[compNo].getCMTTR()*Macros.TIME_SCALE_FACTOR);
			this.failureTime = failureTime;
		}
	}


	class FailureEventComparator implements Comparator<FailureEvent> 
	{
		/*
		 * Sort events in ascending order of failure time
		 */
		@Override
		public int compare(FailureEvent a, FailureEvent b) 
		{
			return Long.compare(a.failureTime,b.failureTime);
		}

	}
}