package org.isw;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
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

	public static IFPacket receive(ServerSocket tcpSocket)
	{
		try
		{
			tcpSocket.setSoTimeout(0);
			Socket tcpSchedSock = tcpSocket.accept();
			tcpSchedSock.setSoTimeout(0);
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
	}
	
	public void send(InetAddress ip, int port) throws IOException{
		Socket socket = new Socket(ip, port);
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(this);
		oos.close();
		socket.close();
	}

	public String toString()
	{
		return "No. of IF opportunities - "+results.length +"\nSchedule - " + jobList.printSchedule() ;
	}
}
