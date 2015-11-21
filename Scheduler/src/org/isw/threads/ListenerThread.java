package org.isw.threads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.util.Enumeration;

import org.isw.FlagPacket;
import org.isw.MachineList;
import org.isw.Macros;
import org.isw.SchedulingDept;

public class ListenerThread extends Thread
{
	MachineList machineList;
	ServerSocket tcpSocket;
	boolean replan;
	int idleMachines = 0;
	public ListenerThread(MachineList machineList)
	{
		this.machineList=machineList;
		replan = false;
	}

	public void run()
	{
		MulticastSocket socket;
		int machineCount = 0;
		try
		{
			socket = new MulticastSocket(Macros.SCHEDULING_DEPT_MULTICAST_PORT);
			socket.joinGroup(InetAddress.getByName(Macros.SCHEDULING_DEPT_GROUP));

			long time = 1;
			boolean flag1 = true;
			while(flag1)
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
					
				case Macros.START_SCHEDULING: 
					// run only once, at beginning
					(new JobSchedThread(machineList,SchedulingDept.days*24/Macros.SHIFT_DURATION)).start();
					flag1 = false;
				}				
			}
			
			tcpSocket = new ServerSocket(Macros.SCHEDULING_DEPT_PORT_TCP_TIMESYNC);
			int completed = 0;
			while(completed<machineList.count())
			{
				FlagPacket timeSyncRequest = FlagPacket.receiveTCP(tcpSocket,0);
				
				switch(timeSyncRequest.flag)
				{
				case Macros.REPLAN:
					//replan
					System.out.format("\n******\nREPLANNING (%d)\n******\n", time);
					replan = true;

				case Macros.REQUEST_TIME:
					++machineCount;
					if(machineCount == machineList.count())
					{
						machineCount = 0;
						//System.out.println("Time: "+time);
						Enumeration<InetAddress> ips = machineList.getIPs();

						if(SchedulingDept.sleepWhileTimeSync)
							sleep(SchedulingDept.sleepTime);

						while(ips.hasMoreElements())
						{
							if(replan)
							{
								//System.out.println("Time replan packet sent: "+time + " mac count: "+machineList.count());
								FlagPacket.sendTCP(Macros.INITIATE_REPLAN, ips.nextElement(), Macros.MACHINE_PORT_TCP);
							}
							else
								FlagPacket.sendTCP(Macros.REPLY_TIME, ips.nextElement(), Macros.MACHINE_PORT_TCP);
						}
						replan = false;
						time++;
					}
					break;
				
				case Macros.PROCESS_COMPLETE:
					completed++;					
				}
			}
		}catch(IOException | InterruptedException e)
		{
			e.printStackTrace();
		}
	}
}