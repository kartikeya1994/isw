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
	static int numOfMachines;

	public static void main(String[] args)
	{

		recd_list = false;

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

		startShift();

		udpSocket.close();

		try 
		{
			tcpSocket.close();
		}catch (IOException e) 
		{
			e.printStackTrace();
		}
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
			
			for(int i = 0; i < numOfMachines; i++)
			{
				IFPacket p = pool.take().get(); //fetch results of all tasks
				System.out.println("Machine " + p.ip + "\n" + p);
				
				ip.add(p.ip);
				port.add(p.port);
				Schedule sched = p.jobList;
				schedule.add(sched);
				
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
				busyTime += (long)row.pmAvgTime;
			}
		}
		System.out.println("PM incorporated schedule- ");
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


}	
