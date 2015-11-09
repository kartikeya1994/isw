package org.isw;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;

public class FlagPacket implements Serializable{

	private static final long serialVersionUID = 5531067608499834803L;
	public int flag;
	public InetAddress ip;
	public int port;
	
	public FlagPacket(int flag)
	{
		this.flag = flag;
	}
	
	public static DatagramPacket makePacket(String host, int port, int flag)
	{
		//prepares UDP datagram 
		final ByteArrayOutputStream baos=new ByteArrayOutputStream();
		final DataOutputStream dos=new DataOutputStream(baos);
		InetAddress group = null;
		try
		{
			dos.writeInt(flag);
			dos.close();
			group = InetAddress.getByName(host);
		} catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		final byte[] buf=baos.toByteArray();
		
		return new DatagramPacket(buf, buf.length, group, port);
	}
	
	public static FlagPacket receiveMulticast(MulticastSocket socket){
		FlagPacket ret = null;
		try
		{
			byte[] bufIn = new byte[256];
			DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
			socket.receive(packet); 
			byte[] data=packet.getData();
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			DataInputStream is = new DataInputStream(in);
			int flag = is.readInt();
			ret = new FlagPacket(flag);
			ret.ip = packet.getAddress();
			ret.port = packet.getPort();
			is.close();
			
		}catch(Exception e)
		{
			System.out.println("Failed to receive FlagPacket.");
			e.printStackTrace();
			
		}
		return ret;
	}
	public static FlagPacket receiveUDP(DatagramSocket socket) throws IOException{
		    FlagPacket ret = null;
			byte[] bufIn = new byte[256];
			DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
			socket.receive(packet); 
			byte[] data=packet.getData();
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			DataInputStream is = new DataInputStream(in);
			int flag = is.readInt();
			ret = new FlagPacket(flag);
			ret.ip = packet.getAddress();
			ret.port = packet.getPort();
			is.close();
			return ret;
	}
	
	public static FlagPacket receiveTCP(ServerSocket tcpSocket, int timeout)
	{
		FlagPacket ret = null;
		
		try
		{
			tcpSocket.setSoTimeout(0);
			Socket tcpSchedSock = tcpSocket.accept();
			tcpSchedSock.setSoTimeout(0);
			ObjectInputStream ois = new ObjectInputStream(tcpSchedSock.getInputStream());
			Object o = ois.readObject();

			if(o instanceof FlagPacket) 
			{

				ret = (FlagPacket)o;
				ret.ip = tcpSchedSock.getInetAddress();
				ret.port = tcpSchedSock.getPort();
			}
			else 
			{
				System.out.println("Received FlagPacket is garbled");
				return null;
			}
			ois.close();
			tcpSchedSock.close();
		}catch(Exception e)
		{
			System.out.println("Failed to receive FlagPacket.");
		}

		return ret;
	}

	public static void sendTCP(int flag, InetAddress ip, int port)
	{
		FlagPacket fp = new FlagPacket(flag);
		try
		{
			Socket socket = new Socket();
			SocketAddress dest = new InetSocketAddress(ip,port);
			socket.connect(dest, 8880000);
			socket.setSoTimeout(0);
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeObject(fp);
			oos.close();
			socket.close();
		}
		catch(Exception e)
		{
			System.out.println("Unable to request for IFPacket to " + ip);
			e.printStackTrace();
		}
	}
}
