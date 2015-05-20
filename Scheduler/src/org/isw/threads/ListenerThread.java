package org.isw.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.isw.MachineList;
import org.isw.Macros;
import org.isw.threads.ClientHandlerThread;

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
		DatagramPacket packet;
		InetAddress group;
		try
		{
			socket = new MulticastSocket(Macros.SCHEDULING_DEPT_PORT);
			group = InetAddress.getByName(Macros.MACHINE_SCHEDULING_GROUP);
			socket.joinGroup(group);

			while(true)
			{
				byte[] bufIn = new byte[256];
				packet = new DatagramPacket(bufIn, bufIn.length);

				socket.receive(packet); 
				ClientHandlerThread worker = new ClientHandlerThread(socket, packet, machineList);
				worker.start();
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	
	}
}