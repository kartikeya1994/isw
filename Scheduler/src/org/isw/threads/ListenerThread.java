package org.isw.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Enumeration;

import org.isw.FlagPacket;
import org.isw.MachineList;
import org.isw.Macros;
import org.isw.SchedulingDept;

public class ListenerThread extends Thread
{
	MachineList machineList;
	boolean replan = false;
	int idleMachines = 0;
	public ListenerThread(MachineList machineList)
	{
		this.machineList=machineList;
	}

	public void run()
	{
		MulticastSocket socket;
		int machineCount = 0;
		try
		{
			socket = new MulticastSocket(Macros.SCHEDULING_DEPT_MULTICAST_PORT);
			socket.joinGroup(InetAddress.getByName(Macros.SCHEDULING_DEPT_GROUP));
			while(!SchedulingDept.processComplete)
			{
				/*
				 * Listen for incoming packets and take following actions
				 */
				FlagPacket fp = FlagPacket.receiveMulticast(socket);
				switch(fp.flag)
				{
				case Macros.REQUEST_SCHEDULING_DEPT_IP:
					// Register incoming machine and reply with own IP
					ClientHandlerThread worker = new ClientHandlerThread(socket, fp, machineList);
					worker.start();
					break;
					
				case Macros.REPLAN:
					//replan
					System.out.println("\n******\nREPLANNING\n******\n");
					replan = true;
					
				case Macros.JOBS_DONE:
					//idleMachines ++;
					
//					if(idleMachines == machineList.count())
//					{
//						System.out.println("All jobs processed");
//						// send end to all machines
//						Enumeration<InetAddress> ips = machineList.getIPs();
//						while(ips.hasMoreElements())
//						{
//							DatagramPacket timeSyncResponse = 
//									FlagPacket.makePacket(ips.nextElement().getHostAddress(), 
//															fp.port, Macros.END_OF_EVERYTHING);
//							socket.send(timeSyncResponse);
//						}	
//						
//						//TODO: Send end to maintenance
//						
//						System.exit(0);
//					}

				case Macros.REQUEST_TIME:
					if(machineCount++ == machineList.count()-1)
					{
						machineCount = 0;
						Enumeration<InetAddress> ips = machineList.getIPs();
						
						if(SchedulingDept.sleepWhileTimeSync)
							sleep(SchedulingDept.sleepTime);
						while(ips.hasMoreElements())
						{
							DatagramPacket timeSyncResponse;
							if(replan)
								timeSyncResponse = FlagPacket.makePacket(ips.nextElement().getHostAddress(), fp.port, Macros.INITIATE_REPLAN);
							else
								timeSyncResponse = FlagPacket.makePacket(ips.nextElement().getHostAddress(), fp.port, Macros.REPLY_TIME);
							socket.send(timeSyncResponse);
						}
						replan = false;
					}
					break;

				case Macros.START_SCHEDULING: 
					// run only once, at beginning
					(new JobSchedThread(machineList,SchedulingDept.days*24/Macros.SHIFT_DURATION)).start();
				}	
			}
			System.out.println("ListenerThread exit");
		}catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}