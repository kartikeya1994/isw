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

public class MaintenanceRequestPacket implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	InetAddress maintenanceIP;
	int maintenancePort;
	public MaintenanceTuple mtTuple;
	
	public InetAddress machineIP;
	public int machinePort;
	
	
	
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
	
	/*public static MaintenanceRequestPacket receiveTCP(ServerSocket tcpSocket, int timeout)
	{
		MaintenanceRequestPacket ret = null;
		try
		{
			tcpSocket.setSoTimeout(timeout);
			Socket tcpMachineSock = tcpSocket.accept();
			ObjectInputStream ois = new ObjectInputStream(tcpMachineSock.getInputStream());
			Object o = ois.readObject();

			if(o instanceof MaintenanceRequestPacket) 
			{

				ret = (MaintenanceRequestPacket)o;
				ret.machineIP = tcpMachineSock.getInetAddress();
				ret.machinePort = tcpMachineSock.getPort();
			}
			else 
			{
				System.out.println("Received MaintenanceRequestPacket is garbled");
				return null;
			}
			ois.close();
			tcpMachineSock.close();
		}catch(Exception e)
		{
			System.out.println("Failed to receive MaintenanceRequestPacket.");
			e.printStackTrace();
		}

		return ret;
	}*/

	public static MaintenanceRequestPacket receiveUDP(DatagramSocket udpSocket, int timeout)
	{
		MaintenanceRequestPacket ret = null;
		try
		{
				udpSocket.setSoTimeout(timeout);
				byte[] bufIn = new byte[4096*400];
				DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
				udpSocket.receive(packet); 
				byte[] data=packet.getData();
				ByteArrayInputStream in = new ByteArrayInputStream(data);
				ObjectInputStream oin = new ObjectInputStream(in); 
				Object o = oin.readObject();
				if(o instanceof MaintenanceRequestPacket) 
				{
				ret = (MaintenanceRequestPacket)o;
				ret.machineIP = packet.getAddress();
				ret.machinePort = packet.getPort();
			}
			else 
			{
				System.out.println("Received MaintenanceRequestPacket is garbled");
				return null;
			}
		}catch(Exception e)
		{
			System.out.println("Failed to receive MaintenanceRequestPacket.");
			e.printStackTrace();
		}

		return ret;
	}
	/*public void sendTCP()
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
	}*/
	public DatagramPacket makePacket() throws IOException{
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream os = new ObjectOutputStream(outputStream);
		os.writeObject(this);
		byte[] object = outputStream.toByteArray();
		return new DatagramPacket(object, object.length,maintenanceIP, maintenancePort);
	}
}
