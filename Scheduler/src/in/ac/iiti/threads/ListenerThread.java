package in.ac.iiti.threads;

import in.ac.iiti.threads.ClientHandlerThread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import org.isw.MachineList;

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
			socket = new MulticastSocket(8888);
			group = InetAddress.getByName("224.0.0.1");
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