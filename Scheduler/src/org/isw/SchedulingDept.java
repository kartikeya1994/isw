package org.isw;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.isw.MachineList;
import org.isw.threads.JobSchedThread;
import org.isw.threads.ListenerThread;

public class SchedulingDept 
{

	public static void main(String args[])
	{
		MachineList machineList = new MachineList();
		ListenerThread listener = new ListenerThread(machineList);
		listener.start();
		System.out.println("Waiting for machines...");
		System.out.println("Press any key to continue...");
		try {
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("here");
		JobSchedThread scheduler = new JobSchedThread(machineList);
		scheduler.start();
	}

	public String myIP() throws SocketException
	{
		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
			NetworkInterface n = (NetworkInterface) e.nextElement();
			Enumeration<InetAddress> ee = n.getInetAddresses();
			while (ee.hasMoreElements())
			{
				InetAddress i = (InetAddress) ee.nextElement();
				String str = i.getHostAddress();
				String parts[]=str.split("\\.");
				if(parts[0].equals("192")&&parts[1].equals("168")||parts[0].equals("10")||parts[0].equals("172")&&Integer.valueOf(parts[1])>=16&&Integer.valueOf(parts[1])<=31)
					return str;
			}
		}
		return "127.0.0.1";
	}

}




