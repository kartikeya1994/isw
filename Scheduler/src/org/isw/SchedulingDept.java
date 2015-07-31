package org.isw;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Scanner;

import org.isw.threads.JobSchedThread;
import org.isw.threads.ListenerThread;

public class SchedulingDept 
{
	public static int days;
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
			socket.close();
			} catch(IOException e){
				
			} 
			}
		listener.start();
		
		
	}

}




