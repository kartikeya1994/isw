package org.isw;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import org.isw.Macros;
import org.isw.threads.ListenerThread;

public class Machine {

	
	static InetAddress serverIP;
	static int machineNo;
	public static Component[] compList;
	public static void main(String[] args) {
		// TODO Auto-generated method stub


		boolean registered=false;
		try
		{
			//create outbound packet with HELLO message
			final ByteArrayOutputStream baos=new ByteArrayOutputStream();
			final DataOutputStream dos=new DataOutputStream(baos);
			dos.writeInt(Macros.REQUEST_SCHEDULING_DEPT_IP);
			dos.close();
			final byte[] buf=baos.toByteArray();
			InetAddress group = InetAddress.getByName(Macros.MACHINE_SCHEDULING_GROUP);
			DatagramPacket packetOut, packetIn;
			packetOut = new DatagramPacket(buf, buf.length, group, Macros.SCHEDULING_DEPT_PORT);

			//create socket
			DatagramSocket socket = new DatagramSocket(Macros.MACHINE_PORT);
			socket.setSoTimeout(3000);

			while(!registered)
			{
				System.out.println("Finding server...");
				socket.send(packetOut);

				byte[] resbuf = new byte[256];
				packetIn = new DatagramPacket(resbuf, resbuf.length);
				try
				{
					socket.receive(packetIn); //blocking call for 1000ms
				}catch(SocketTimeoutException stoe)
				{
					System.out.println("Timed out.");
					continue;
				}
				final byte[] reply=packetIn.getData();
				final ByteArrayInputStream bais=new ByteArrayInputStream(reply);
				final DataInputStream dis=new DataInputStream(bais);

				if(dis.readInt() == Macros.REPLY_SCHEDULING_DEPT_IP)
				{
					registered=true;
					serverIP=packetIn.getAddress();
					System.out.println("Connected to server: "+serverIP.toString());

					//socket.close();
				}
				else
					continue;
			}
			machineNo = Integer.parseInt(args[1]);
			compList = parseExcel(machineNo);
			ListenerThread listener = new ListenerThread(serverIP,socket);
			listener.start();
			
			
		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	private static Component[] parseExcel(int machineNo) {
		/**
		 * Parse the component excel file into a list of components.
		 * Total number of components should be 14 for our experiment.
		 * Different component excel file for different machineNo (Stick
		 * to one for now)
		 * **/
		return null;
	}

}