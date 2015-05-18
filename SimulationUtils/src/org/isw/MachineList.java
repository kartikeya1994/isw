package org.isw;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;

public class MachineList implements Serializable
{
	private static final long serialVersionUID = 2560671706641927632L;
	Hashtable<InetAddress, Long> ips ;
	int count;
	public InetAddress senderIP;

	public MachineList()
	{
		ips = new Hashtable<InetAddress, Long>();
		count = 0;
	}

	public boolean contains(InetAddress ip)
	{
		return ips.containsKey(ip);
	}

	public synchronized void add(InetAddress ip, long num)
	{
		count++;
		ips.put(ip, new Long(num));
	}

	public int count()
	{
		return count;
	}

	public Enumeration<InetAddress> getIPs()
	{
		return ips.keys();
	}
	
	public Enumeration<Long> getPorts()
	{
		return ips.elements();
	}

	public static MachineList receive(ServerSocket tcpSocket)
	{
		//uses TCP to receive machine list
		//
		MachineList ret = null;
		try
		{
			Socket tcpSchedSock = tcpSocket.accept();
			ObjectInputStream ois = new ObjectInputStream(tcpSchedSock.getInputStream());
			Object o = ois.readObject();

			if(o instanceof MachineList) 
			{

				ret = (MachineList)o;
				ret.senderIP = tcpSchedSock.getInetAddress();
			}
			else 
			{
				System.out.println("Received Machine List is garbled");
			}
			ois.close();
			tcpSchedSock.close();
		}catch(Exception e)
		{
			System.out.println("Failed to receive machine list.");
		}

		return ret;
	}

	public static void send(MachineList list, InetAddress ip, int port)
	{
		try
		{
			Socket socket = new Socket(ip, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(list);
			oos.close();
			socket.close();
		}
		catch(Exception e)
		{
			System.out.println("Unable to send machine list");
			e.printStackTrace();
		}
	}

	public String toString()
	{
		return ips.toString();
	}
}