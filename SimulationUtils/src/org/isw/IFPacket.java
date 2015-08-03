package org.isw;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class IFPacket implements Serializable
{
	private static final long serialVersionUID = -8976546963278613092L;
	public SimulationResult results[];
	public Component[] compList;
	public Schedule jobList;
	public InetAddress ip;
	public int port;

	public IFPacket(SimulationResult results[], Schedule jobList, Component[] compList)
	{
		this.results = results;
		this.jobList = jobList;
		this.compList = compList;
	}

	/*public static IFPacket receive(ServerSocket tcpSocket)
	{
		try
		{
			Socket tcpSchedSock = tcpSocket.accept();
			ObjectInputStream ois = new ObjectInputStream(tcpSchedSock.getInputStream());
			Object o = ois.readObject();
			if(o instanceof IFPacket) 
			{
				IFPacket ret =  (IFPacket)o;
				ret.ip = tcpSchedSock.getInetAddress();
				ret.port = tcpSchedSock.getPort();
				return ret;
			}
			else 
			{
				System.out.println("Couldn't understand packet received from machine");
			}
			ois.close();
			tcpSchedSock.close();
		}catch(Exception e)
		{
			System.out.println("Failed to receive packet from machine.");
		}
		return null;
	}*/
	public static IFPacket receiveUDP(DatagramSocket udpSocket){
		IFPacket ret = null;
		try
		{
				byte[] bufIn = new byte[4096*400];
				DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
				udpSocket.receive(packet); 
				byte[] data=packet.getData();
				ByteArrayInputStream in = new ByteArrayInputStream(data);
				ObjectInputStream oin = new ObjectInputStream(in); 
				Object o = oin.readObject();
				if(o instanceof IFPacket) 
				{
				ret = (IFPacket)o;
				ret.ip = packet.getAddress();
				ret.port = packet.getPort();
			}
			else 
			{
				System.out.println("Received IF packet is garbled");
				return null;
			}
		}catch(Exception e)
		
		{
			System.out.println("Failed to receive IFPacket.");
			e.printStackTrace();
		}
		return ret;
	}
	/*public void send(InetAddress ip, int port) throws IOException{
		Socket socket = new Socket(ip, port);
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(this);
		oos.close();
		socket.close();
	}*/
	public DatagramPacket makePacket(InetAddress ip, int port) throws IOException{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(outputStream);
		os.writeObject(this);
		byte[] object = outputStream.toByteArray();
		return new DatagramPacket(object, object.length,ip, port);
	}
	public String toString()
	{
		return "No. of IF opportunities - "+results.length +"\nSchedule - " + jobList.printSchedule() ;
	}
}
