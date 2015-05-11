package com.mb14;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Hashtable;

public class MachineList
{
	Hashtable<InetAddress, Long> ips ;
	int count;

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

	public Enumeration<InetAddress> getList()
	{
		return ips.keys();
	}
}