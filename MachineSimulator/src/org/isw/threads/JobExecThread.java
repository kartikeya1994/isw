package org.isw.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import org.isw.Component;
import org.isw.FlagPacket;
import org.isw.Job;
import org.isw.Logger;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.MaintenanceRequestPacket;
import org.isw.MaintenanceTuple;
import org.isw.Schedule;

public class JobExecThread extends Thread{
	Schedule jobList;
	DatagramSocket socket;
	DatagramPacket timePacket;
	ServerSocket tcpSocket;
	InetAddress maintenanceIP;
	public JobExecThread(Schedule jobList , DatagramSocket socket, ServerSocket tcpSocket, InetAddress maintenanceIP){
		this.jobList = jobList;
		this.socket = socket;
		this.tcpSocket = tcpSocket;
		this.maintenanceIP = maintenanceIP;
		timePacket = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.REQUEST_TIME);
	}

	public void run()
	{
		Machine.setStatus(Machine.getOldStatus());
		long time=0;

		// find all machine failures and CM times for this shift
		LinkedList<FailureEvent> failureEvents = new LinkedList<FailureEvent>();
		FailureEvent upcomingFailure = null;
		for(int compNo=0; compNo<Machine.compList.length; compNo++)
		{
			long ft = (long)Machine.compList[compNo].getCMTTF();
			if(ft < Macros.SHIFT_DURATION)
			{
				// this component fails in this shift
				failureEvents.add(new FailureEvent(compNo, ft*Macros.TIME_SCALE_FACTOR));
			}
		}
		if(!failureEvents.isEmpty())
		{
			Collections.sort(failureEvents, new FailureEventComparator());
			upcomingFailure =  failureEvents.pop();
		}

		while(!jobList.isEmpty() && time < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR)
		{
			
			System.out.println("Machine Status: "+Machine.getStatus());

			Job current = jobList.peek(); 

			/*
			 * Perform action according to what job is running
			 * Increment costs or wait for labour to arrive for CM/PM
			 */
			if(current.getJobType()!= Job.JOB_CM&&current.getJobType()!= Job.JOB_PM && upcomingFailure!=null && time == upcomingFailure.failureTime)
			{
				/*
				 * Machine fails. 
				 * Add CM job to top of schedule and run it. 
				 */
				System.out.println("Machine Failed. Requesting maintenance...");
				Logger.log(Machine.getStatus(), "Machine Failed. Requesting maintenance...");
				Job cmJob = new Job("CM", upcomingFailure.repairTime, Machine.compList[upcomingFailure.compNo].getCMLabourCost(), Job.JOB_CM);
				cmJob.setFixedCost(Machine.compList[upcomingFailure.compNo].getCMFixedCost());
				cmJob.setCompNo(upcomingFailure.compNo);
				jobList.addJobTop(cmJob);
				Machine.setStatus(Macros.MACHINE_WAITING_FOR_CM_LABOUR);
				Logger.log(Machine.getStatus(), "");
				current = jobList.peek();
			}

			if(Machine.getStatus() == Macros.MACHINE_WAITING_FOR_CM_LABOUR || Machine.getStatus() == Macros.MACHINE_WAITING_FOR_PM_LABOUR)
			{
				// see if maintenance labour is available at this time instant
				int[] labour_req = null;
				// determine labour requirement
				if(current.getJobType() == Job.JOB_CM)
					labour_req = Machine.compList[current.getCompNo()].getCMLabour();
				else if(current.getJobType() == Job.JOB_PM)
					labour_req = Machine.compList[current.getCompNo()].getPMLabour();
				System.out.format("Labour: %d %d %d", labour_req[0],labour_req[1],labour_req[2]);
				// send labour request
				MaintenanceTuple mtTuple;
				if(Machine.getStatus() == Macros.MACHINE_WAITING_FOR_CM_LABOUR)
					mtTuple = new MaintenanceTuple(time, time+current.getJobTime(), labour_req);
				else
				{
					mtTuple = new MaintenanceTuple(time, time+current.getSeriesTTR(), current.getSeriesLabour());
				}

				System.out.format("Labour: %d %d %d", mtTuple.labour[0],mtTuple.labour[1],mtTuple.labour[2]);

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
					Logger.log(Machine.getStatus(), "Request granted");
					continue;
				}
				else if(flagPacket.flag == Macros.LABOUR_DENIED)
				{
					System.out.println("Request denied. Not enough labour");

					Logger.log(Machine.getStatus(),"Request denied. Not enough labour");
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
				Machine.setStatus(Macros.MACHINE_RUNNING_JOB);
				current.setStatus(Job.STARTED);
				Logger.log(Machine.getStatus(), "");
				// no failure, no maintenance. Just increment cost models normally.
				//Machine.procCost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				for(Component comp : Machine.compList)
					comp.initAge++;
				Machine.runTime++;
			}

			else if(current.getJobType() == Job.JOB_PM)
			{
				if(Machine.getStatus() != Macros.MACHINE_PM)
				{
					// request PM if labours not yet allocated
					Machine.setStatus(Macros.MACHINE_WAITING_FOR_PM_LABOUR);
					Logger.log(Machine.getStatus(), "");
					continue;
				}
				System.out.println(current.getJobTime());
				// since an actual PM job is a series of PM jobs of each comp in compCombo
				// we set all jobs in series to SERIES_STARED
				if(current.getStatus() == Job.NOT_STARTED)
				{
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
				current.setStatus(Job.STARTED);
				Machine.cmCost += current.getFixedCost() + current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				Machine.downTime++;
				Machine.cmDownTime++;
			}

			// decrement job time by unit time
			try{
				if(Machine.getStatus()==Macros.MACHINE_RUNNING_JOB || Machine.getStatus()==Macros.MACHINE_CM || Machine.getStatus()==Macros.MACHINE_PM)
					jobList.decrement(1);
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

					Component comp1 = Machine.compList[current.getCompNo()];
					comp1.initAge = (1-comp1.pmRF)*comp1.initAge;
					Machine.compPMJobsDone[current.getCompNo()]++;


					Machine.pmJobsDone++;

					// let maintenance know how much labour has been released (for logging purpose only)
					if(jobList.getSize()<=1 || jobList.jobAt(1).getStatus()!=Job.SERIES_STARTED)
					{
						MaintenanceTuple release = new MaintenanceTuple(-2, 0, current.getSeriesLabour());
						MaintenanceRequestPacket mrp = new MaintenanceRequestPacket(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP, release);
						mrp.sendTCP();
					}

					// recompute component failures
					failureEvents = new LinkedList<FailureEvent>();
					upcomingFailure = null;
					for(int compNo=0; compNo<Machine.compList.length; compNo++)
					{
						long ft = time + (long) Machine.compList[compNo].getCMTTF()*Macros.TIME_SCALE_FACTOR;
						if(ft < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR)
						{
							// this component fails in this shift
							failureEvents.add(new FailureEvent(compNo, ft));
						}
					}

					if(!failureEvents.isEmpty())
					{
						Collections.sort(failureEvents, new FailureEventComparator());
						upcomingFailure =  failureEvents.pop();
					}
					break;

				case Job.JOB_CM:
					// 
					Component comp = Machine.compList[current.getCompNo()];
					comp.initAge = (1 - comp.cmRF)*comp.initAge;
					Machine.cmJobsDone++;
					Machine.compCMJobsDone[current.getCompNo()]++;
					// let maintenance know how much labour has been released (for logging purpose only)
					MaintenanceTuple release = new MaintenanceTuple(-2, 0, comp.getCMLabour());
					MaintenanceRequestPacket mrp = new MaintenanceRequestPacket(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP, release);
					mrp.sendTCP();
					// recompute component failures
					failureEvents = new LinkedList<FailureEvent>();
					upcomingFailure = null;
					for(int compNo=0; compNo<Machine.compList.length; compNo++)
					{
						long ft = time + (long) Machine.compList[compNo].getCMTTF()*Macros.TIME_SCALE_FACTOR;
						if(ft < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR)
						{
							// this component fails in this shift
							failureEvents.add(new FailureEvent(compNo, ft));
						}
					}
					if(!failureEvents.isEmpty())
					{
						Collections.sort(failureEvents, new FailureEventComparator());
						upcomingFailure =  failureEvents.pop();
					}
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
					Logger.log(Machine.getStatus(), "Job "+ job.getJobName()+" complete");
				}
				catch(IOException e){
					e.printStackTrace();
					System.exit(0);
				}
			}
			try {
				byte[] bufIn = new byte[128];
				DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
				socket.send(timePacket);
				socket.receive(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if(jobList.isEmpty()){
			Machine.idleTime += Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR - time;
			return;
		}
		int i = jobList.indexOf(jobList.peek());
		while(i < jobList.getSize()){
			Machine.penaltyCost += jobList.jobAt(i++).getPenaltyCost()*Macros.SHIFT_DURATION;
		}
		Logger.log(Machine.getStatus(), "Shift has ended");

	}


	class FailureEvent
	{
		public int compNo;
		public long repairTime;
		public long failureTime;

		public FailureEvent(int compNo, long failureTime)
		{
			this.compNo = compNo;
			this.repairTime = Component.notZero(Machine.compList[compNo].getCMTTR()*Macros.TIME_SCALE_FACTOR);
			this.failureTime = failureTime;
		}
	}

	class FailureEventComparator implements Comparator<FailureEvent> {
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