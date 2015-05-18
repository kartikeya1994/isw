package org.isw.threads;

import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.isw.Schedule;

public class SendScheduleTask implements Runnable
{
	Schedule s;
	InetAddress ip;
	int port;
	
	public SendScheduleTask(Schedule s, InetAddress ip, int port)
	{
		this.s = s;
		this.ip = ip;
		this.port = port;
	}
	@Override
	public void run() 
	{
		try
		{
			Socket socket = new Socket(ip, port);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(s);
			oos.close();
			socket.close();
		}
		catch(Exception e)
		{
			System.out.println("Unable to send PM incorporated schedule to machine "+ip);
			e.printStackTrace();
		}
	}
	
}
