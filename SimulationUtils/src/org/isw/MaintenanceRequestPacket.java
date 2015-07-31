package org.isw;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class MaintenanceRequestPacket 
{
	InetAddress maintenanceIP;
	int maintenancePort;
	MaintenanceTuple mtTuple;
	
	public MaintenanceRequestPacket(InetAddress maintenanceIP, int maintenancePort, MaintenanceTuple mtTuple)
	{
		this.maintenanceIP = maintenanceIP;
		this.maintenancePort = maintenancePort;
		this.mtTuple = mtTuple;
	}
	
	public MaintenanceRequestPacket(MaintenanceTuple mtTuple)
	{
		this.mtTuple = mtTuple;
	}
	
	public static MaintenanceTuple receiveTCP(ServerSocket tcpSocket, int timeout)
	{
		MaintenanceRequestPacket ret = null;
		try
		{
			tcpSocket.setSoTimeout(timeout);
			Socket tcpSchedSock = tcpSocket.accept();
			ObjectInputStream ois = new ObjectInputStream(tcpSchedSock.getInputStream());
			Object o = ois.readObject();

			if(o instanceof MaintenanceRequestPacket) 
			{

				ret = (MaintenanceRequestPacket)o;
			}
			else 
			{
				System.out.println("Received MaintenanceRequestPacket is garbled");
				return null;
			}
			ois.close();
			tcpSchedSock.close();
		}catch(Exception e)
		{
			System.out.println("Failed to receive MaintenanceRequestPacket.");
		}

		return ret.mtTuple;
	}

	public void sendTCP()
	{
		try
		{
			Socket socket = new Socket();
			SocketAddress dest = new InetSocketAddress(maintenanceIP,maintenancePort);
			socket.connect(dest, 88000);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(this);
			oos.close();
			socket.close();
		}
		catch(Exception e)
		{
			System.out.println("Unable to send Maintenance Request to " + maintenanceIP);
			e.printStackTrace();
		}
	}
}
