package org.isw.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.isw.FlagPacket;
import org.isw.Logger;
import org.isw.MachineList;
import org.isw.Macros;

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
		try
		{
			socket = new MulticastSocket(Macros.MAINTENANCE_DEPT_MULTICAST_PORT);
			socket.joinGroup(InetAddress.getByName(Macros.MAINTENANCE_DEPT_GROUP));
			while(true)
			{
				FlagPacket fp = FlagPacket.receiveMulticast(socket);
				if(fp.flag ==Macros.REQUEST_MAINTENANCE_DEPT_IP){
					if(fp.port == Macros.SCHEDULING_DEPT_PORT){
						DatagramPacket packetOut =FlagPacket.makePacket(fp.ip.getHostAddress(), fp.port, Macros.REPLY_MAINTENANCE_DEPT_IP);	
						socket.send(packetOut);
					}
					else{
					// listen for machines trying to connect
					ClientHandlerThread worker = new ClientHandlerThread(socket, fp, machineList);
					worker.start();	
					}
				}
				
				else if (fp.flag == Macros.START_MAINTENANCE_PLANNING)
				{
					Logger.connect();
					// start planning after receiving signal from central logging
					MaintenanceThread mt = new MaintenanceThread(machineList);
					mt.start();
				}
				
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	
	}
}