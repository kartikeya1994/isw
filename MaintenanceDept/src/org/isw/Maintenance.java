package org.isw;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.isw.threads.ListenerThread;


public class Maintenance 
{

	static DatagramSocket socket;
	
	

	public static void main(String[] args)
	{
		Macros.loadMacros();
		try {
			socket = new DatagramSocket(Macros.MAINTENANCE_DEPT_PORT);
			boolean registered = false;
			DatagramPacket iswPacket = FlagPacket.makePacket(Macros.ISW_GROUP, Macros.ISW_MULTICAST_PORT, Macros.REQUEST_ISW_IP|Macros.MAINTENANCE_DEPT_FLAG);
			while(!registered){
				socket.send(iswPacket);

				FlagPacket packetIn;
				try
				{
					packetIn = FlagPacket.receiveUDP(socket);
				}catch(SocketTimeoutException stoe)
				{
					System.out.println("Timed out.");
					continue;
				}
				
				if (packetIn.flag == Macros.REPLY_ISW_IP){
					socket.close();
					registered=true;
					Logger.init(packetIn.ip);
				}
				else
					continue;
			}
		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		MachineList machineList = new MachineList();
		ListenerThread listener = new ListenerThread(machineList);
		listener.start();
	}
		
}	
	