package org.isw;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.isw.threads.ListenerThread;

public class SchedulingDept 
{
	public static int days;
	public static InetAddress maintenanceIP;
	public static void main(String args[])
	{
		DatagramSocket socket = null;
		
		try {
			boolean registered = false;
			socket = new DatagramSocket(Macros.SCHEDULING_DEPT_PORT);
			DatagramPacket iswPacket = FlagPacket.makePacket(Macros.ISW_GROUP, Macros.ISW_MULTICAST_PORT, Macros.REQUEST_ISW_IP|Macros.SCHEDULING_DEPT_FLAG);
			while(!registered){
				// register with central logging
				socket.send(iswPacket);
				FlagPacket packetIn;
				try
				{
					packetIn = FlagPacket.receiveUDP(socket); //blocking call for 1000ms
				}catch(SocketTimeoutException stoe)
				{
					System.out.println("Timed out.");
					continue;
				}
				if (packetIn.flag == Macros.REPLY_ISW_IP){
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
				System.out.println("Timed out.");
				continue; 
			} 
			byte[] reply=packet.getData();
			final ByteArrayInputStream bais=new ByteArrayInputStream(reply);
			DataInputStream dias =new DataInputStream(bais);
			Macros.SHIFT_DURATION = dias.readInt();
			days = dias.readInt();
			Macros.TIME_SCALE_FACTOR = dias.readInt();
			init = true;
			
			} catch(IOException e){
				
			} 
			}
		try {
			socket.setSoTimeout(0);
			DatagramPacket fp = FlagPacket.makePacket(Macros.MAINTENANCE_DEPT_GROUP, Macros.MAINTENANCE_DEPT_MULTICAST_PORT,Macros.REQUEST_MAINTENANCE_DEPT_IP );
			socket.send(fp);
			FlagPacket reply = FlagPacket.receiveUDP(socket);
			maintenanceIP = reply.ip;
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		listener.start();
		
		
	}

}




