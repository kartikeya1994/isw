package org.isw.threads;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.Callable;

import org.isw.IFPacket;

public class FetchIFTask implements Callable<IFPacket>
{
	InetAddress ip;
	int port;
	ServerSocket tcpSocket;
	IFPacket result = null;
	
	public FetchIFTask(ServerSocket tcpSocket, InetAddress ip, int port)
	{
		this.ip = ip;
		this.port = port;
		this.tcpSocket = tcpSocket;
	}

	@Override
	public IFPacket call() throws Exception {
		System.out.println(port);
		return IFPacket.receive(tcpSocket); //blocking call that waits for reply
	}
	
}
