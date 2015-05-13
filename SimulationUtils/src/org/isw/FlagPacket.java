package org.isw;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;

public class FlagPacket {
	int flag;
	
	public static DatagramPacket makePacket(String host, int port, int flag)
	{
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
}
