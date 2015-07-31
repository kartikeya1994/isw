package org.isw.threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.FlagPacket;
import org.isw.IFPacket;
import org.isw.Job;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;
import org.isw.SimulationResult;

public class ListenerThread extends Thread
{
	Schedule jl;
	InetAddress schedulerIP;
	DatagramSocket udpSocket;
	ServerSocket tcpSocket;
	private InetAddress maintenanceIP =null;

	public ListenerThread(InetAddress schedulerIP, InetAddress maintenanceIP, DatagramSocket udpSocket,ServerSocket tcpSocket) {
		this.schedulerIP = schedulerIP;
		this.udpSocket = udpSocket;
		this.tcpSocket = tcpSocket;
		this.maintenanceIP = maintenanceIP;
	}

	public void run()
	{
		DatagramPacket packet;

		try {
			udpSocket.setSoTimeout(0);	
			jl = new Schedule(InetAddress.getByName("localhost")); //FIXME: why is this localhost?
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		try
		{
			while(true)
			{
				byte[] bufIn = new byte[4096*8];
				packet = new DatagramPacket(bufIn, bufIn.length);
				//Receive signals from Scheduling Dept
				udpSocket.receive(packet); 
				byte[] reply=packet.getData();
				byte [] header = Arrays.copyOfRange(reply, 0, 4);
				final ByteArrayInputStream bais=new ByteArrayInputStream(header);
				DataInputStream dias =new DataInputStream(bais);
				int action = dias.readInt();
				switch(action) // incoming packet can lead to three actions: 
				{
				/*
				 * Machine requests for schedule of next shift (REQUEST_NEXT_SHIFT)
				 * Scheduler requests for 
				 */
				case Macros.PROCESS_COMPLETE:
					// simulation is over
					writeResults();
					break;
				case Macros.REPLY_NEXT_SHIFT:	
					// Got schedule for next shift
					byte[] data = Arrays.copyOfRange(reply, 4, reply.length);
					ByteArrayInputStream in = new ByteArrayInputStream(data);
					ObjectInputStream is = new ObjectInputStream(in);
					try {
						//Parse schedule from packet.
						jl = (Schedule) is.readObject();	
						System.out.println("Received schedule from scheduler:" + jl.printSchedule());
						System.out.println("Total time" + jl.getSum());
						System.out.println("Running Simulations");
						long starttime = System.currentTimeMillis();
						/* TODO:
						 * Get simulation results by executing Memetic Algorithm
						 * */
						SimulationResult[] results = null;

						System.out.println("Simulations complete in " +(System.currentTimeMillis() - starttime));
						System.out.println("Sending simulation results to Maintenance");
						
						//Send simulation results to Maintenance Dept.
						IFPacket ifPacket =  new IFPacket(results,jl,Machine.compList);
						ifPacket.send(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP);
						
						//receive PM incorporated schedule from maintenance
						jl = Schedule.receive(tcpSocket); 
						System.out.println("Received schedule from maintenance:" + jl.printSchedule());
						System.out.println("Total time" + jl.getSum());
						
						//Execute schedule received by maintenance
						ExecutorService threadPool = Executors.newSingleThreadExecutor();
						threadPool.execute(new JobExecThread(jl, udpSocket, tcpSocket, maintenanceIP));
						threadPool.shutdown();
						while(!threadPool.isTerminated()); 
						Machine.shiftCount++;
						
						//Request Scheduling Dept for next shift
						FlagPacket.sendTCP(Macros.REQUEST_NEXT_SHIFT, schedulerIP, Macros.SCHEDULING_DEPT_PORT_TCP);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
					break;
					
				case Macros.REQUEST_PREVIOUS_SHIFT:
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
					
					//send jobs pending from last shift to scheduler
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					ObjectOutputStream os = new ObjectOutputStream(outputStream);
					os.writeObject(jl);
					byte[] object = outputStream.toByteArray();
					DatagramPacket sendPacket = new DatagramPacket(object, object.length,schedulerIP, Macros.SCHEDULING_DEPT_PORT);
					udpSocket.send(sendPacket);
				}
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private void writeResults() {
		System.out.println("=========================================");
		System.out.println("Downtime:" + String.valueOf(Machine.downTime*100/(Machine.runTime)) +"%");
		System.out.println("CM Downtime: "+ Machine.cmDownTime +" hours");
		System.out.println("PM Downtime: "+ Machine.pmDownTime +" hours");
		System.out.println("Waiting Downtime: "+ Machine.waitTime +" hours");
		System.out.println("Machine Idle time: "+ Machine.idleTime+" hours");
		System.out.println("PM Cost: "+ Machine.pmCost);
		System.out.println("CM Cost: "+ Machine.cmCost);
		System.out.println("Penalty Cost: "+ Machine.penaltyCost);
		System.out.println("Processing Cost: "+ Machine.procCost);
		System.out.println("Number of jobs:" + Machine.jobsDone);
		System.out.println("Number of CM jobs:" + Machine.cmJobsDone);
		System.out.println("Number of PM jobs:" + Machine.pmJobsDone);
		for(int i=0 ;i<Machine.compList.length; i++)
			System.out.println("Component "+String.valueOf(i+1)+": PM "+Machine.compPMJobsDone[i]+"|CM"+Machine.compCMJobsDone[i]);

	}
}