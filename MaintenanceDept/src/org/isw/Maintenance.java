package org.isw;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;

import org.isw.threads.ListenerThread;


public class Maintenance 
{
	/*
	 * Register with central logging and start listener thread
	 */

	static DatagramSocket socket;
	public static int maxLabour[] = new int[3];
	public static void main(String[] args)
	{
		try 
		{
			socket = new DatagramSocket(Macros.MAINTENANCE_DEPT_PORT);
			socket.setSoTimeout(3000);
			boolean registered = false;
			DatagramPacket iswPacket = FlagPacket.makePacket(Macros.ISW_GROUP, Macros.ISW_MULTICAST_PORT, Macros.REQUEST_ISW_IP|Macros.MAINTENANCE_DEPT_FLAG);
			while(!registered){

				//register with central logging
				socket.send(iswPacket);

				FlagPacket packetIn;
				try
				{
					packetIn = FlagPacket.receiveUDP(socket);
				}catch(SocketTimeoutException stoe)
				{
					continue;
				}

				if (packetIn.flag == Macros.REPLY_ISW_IP){
					registered=true;
					Logger.init(packetIn.ip);
				}
				else
					continue;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		MachineList machineList = new MachineList();
		ListenerThread listener = new ListenerThread(machineList);
		listener.start();
		boolean init = false;
		while(!init){
			try{
				byte[] bufIn = new byte[4096*8];
				DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
				//Receive signals from Scheduling Dept
				try
				{
					socket.receive(packet);
				}
				catch(SocketTimeoutException stoe) {
					continue; 
				} 
				byte[] reply=packet.getData();
				final ByteArrayInputStream bais=new ByteArrayInputStream(reply);
				DataInputStream dias =new DataInputStream(bais);
				Macros.SHIFT_DURATION = dias.readInt();
				Macros.TIME_SCALE_FACTOR = dias.readInt();
				Macros.SIMULATION_COUNT = dias.readInt();
				maxLabour[0] = dias.readInt();
				maxLabour[1] = dias.readInt();
				maxLabour[2] = dias.readInt();
				socket.close();
				init = true;
			} catch(IOException e){

			} 
		}

	}

}	
