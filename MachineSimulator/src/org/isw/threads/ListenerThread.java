package org.isw.threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;

import org.isw.Schedule;

public class ListenerThread extends Thread
{

	final static int SCHED_PUT = 3;
	final static int SCHED_GET = 4;
	Schedule jl;
	InetAddress serverIP;
	DatagramSocket socket;
	public ListenerThread(InetAddress serverIP,DatagramSocket socket) {
		this.serverIP = serverIP;
		this.socket = socket;
	}
	public void run()
	{
		
		DatagramPacket packet;
		
		try {
			socket.setSoTimeout(0);
			jl = new Schedule(InetAddress.getByName("localhost"));
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try
		{
			while(true)
			{
				byte[] bufIn = new byte[1024];
				packet = new DatagramPacket(bufIn, bufIn.length);
				socket.receive(packet); 
				byte[] reply=packet.getData();
				byte [] header = Arrays.copyOfRange(reply, 0, 4);
				final ByteArrayInputStream bais=new ByteArrayInputStream(header);
				DataInputStream dias =new DataInputStream(bais);
				int action = dias.readInt();
				if(action==SCHED_PUT){
					byte[] data = Arrays.copyOfRange(reply, 4, reply.length);
					ByteArrayInputStream in = new ByteArrayInputStream(data);
					ObjectInputStream is = new ObjectInputStream(in);
					try {
						jl = (Schedule) is.readObject();	
						System.out.println("Received schedule:" + jl.printSchedule());

					} catch (ClassNotFoundException e) {
					        e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else if(action==SCHED_GET){
					
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		            ObjectOutputStream os = new ObjectOutputStream(outputStream);
		            os.writeObject(jl);
		            byte[] object = outputStream.toByteArray();
		            DatagramPacket sendPacket = new DatagramPacket(object, object.length,serverIP, 8889);
		            socket.send(sendPacket);
				}
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
}