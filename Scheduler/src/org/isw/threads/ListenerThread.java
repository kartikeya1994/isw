package org.isw.threads;

import java.io.IOException;
import java.net.DatagramPacket;
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
					// COMPLETE THIS
					if(machineCount++ == machineList.count()){
						machineCount = 0;
						DatagramPacket timePacket = FlagPacket.makePacket(fp.ip.getHostAddress(), fp.port, Macros.REPLY_TIME);
						socket.send(timePacket);
					}
				}	
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
}