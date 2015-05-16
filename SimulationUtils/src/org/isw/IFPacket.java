package org.isw;

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

public class IFPacket implements Serializable
{
	private static final long serialVersionUID = -8976546963278613092L;
	float IF;
	Schedule jobList;

	public IFPacket(float IF, Schedule jobList)
	{
		this.IF = IF;
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
				return (IFPacket)o;
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
}
