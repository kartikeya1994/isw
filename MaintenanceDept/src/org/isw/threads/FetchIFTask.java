package org.isw.threads;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.Callable;

import org.isw.IFPacket;

public class FetchIFTask implements Callable<IFPacket>
{
	InetAddress ip;
	int port;
	DatagramSocket udpSocket;
	IFPacket result = null;
	
	public FetchIFTask(DatagramSocket udpSocket, InetAddress ip, int port)
	{
		this.ip = ip;
		this.port = port;
		this.udpSocket = udpSocket;
	}

	@Override
	public IFPacket call() throws Exception {
		System.out.println(port);
		return IFPacket.receiveUDP(udpSocket); //blocking call that waits for reply
	}
	
}
