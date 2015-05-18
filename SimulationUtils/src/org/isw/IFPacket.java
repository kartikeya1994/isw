package org.isw;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class IFPacket implements Serializable
{
	private static final long serialVersionUID = -8976546963278613092L;
	SimulationResult results[];
	public Schedule jobList;
	public InetAddress ip;
	public long port;

	public IFPacket(SimulationResult results[], Schedule jobList)
	{
		this.results = results;
		this.jobList = jobList;
	}

	public static IFPacket receive(ServerSocket tcpSocket)
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
	}
	
	public String toString()
	{
		return "No. of IF opportunities - "+results.length +"\nSchedule - " + jobList.printSchedule() ;
	}
}
