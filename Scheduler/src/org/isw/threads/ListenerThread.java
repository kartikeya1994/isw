package org.isw.threads;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.isw.FlagPacket;
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
			socket = new MulticastSocket(Macros.SCHEDULING_DEPT_MULTICAST_PORT);
			socket.joinGroup(InetAddress.getByName(Macros.MACHINE_SCHEDULING_GROUP));
			socket.joinGroup(InetAddress.getByName(Macros.MAINTENANCE_SCHEDULING_GROUP));
			while(true)
			{
				FlagPacket fp = FlagPacket.receiveMulticast(socket);
				switch(fp.flag){
				case Macros.REQUEST_SCHEDULING_DEPT_IP:
					ClientHandlerThread worker = new ClientHandlerThread(socket, fp, machineList);
					worker.start();
					break;
				case Macros.REQUEST_MACHINE_LIST:
					System.out.println("Request for machineList");
					MachineList.send(machineList, fp.ip, fp.port);
				}	
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	
	}
}