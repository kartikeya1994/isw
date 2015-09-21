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
			while(true)
			{
				/*
				 * Listen for incoming packets and take following actions
				 */
				FlagPacket fp = FlagPacket.receiveMulticast(socket);
				switch(fp.flag){
					case Macros.REQUEST_SCHEDULING_DEPT_IP:
						// Register incoming machine and reply with own IP
						ClientHandlerThread worker = new ClientHandlerThread(socket, fp, machineList);
						worker.start();
					break;
					case Macros.REQUEST_TIME:
						if(machineCount++ == machineList.count()-1){
						machineCount = 0;
						Enumeration<InetAddress> ips = machineList.getIPs();
						//sleep(1000);
						while(ips.hasMoreElements()){
								DatagramPacket timePacket = FlagPacket.makePacket(ips.nextElement().getHostAddress(), fp.port, Macros.REPLY_TIME);
								socket.send(timePacket);
							
							}
						}
						break;
					case Macros.START_SCHEDULING:
						(new JobSchedThread(machineList,SchedulingDept.days*24/Macros.SHIFT_DURATION)).start();
				}	
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}