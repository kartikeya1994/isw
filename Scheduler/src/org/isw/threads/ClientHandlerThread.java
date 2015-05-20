package org.isw.threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import org.isw.MachineList;
import org.isw.Macros;

class ClientHandlerThread extends Thread {

	MulticastSocket socket;
	DatagramPacket packet;
	MachineList machineList;
	public ClientHandlerThread(MulticastSocket socket, DatagramPacket packet, MachineList machineList) 
	{
		this.socket=socket;
		this.packet=packet;
		this.machineList=machineList;
	}

	@Override
	public void run() {
		final byte[] reply=packet.getData();
		final ByteArrayInputStream bais=new ByteArrayInputStream(reply);
		final DataInputStream dais=new DataInputStream(bais);
		try
		{
			if(dais.readInt()==Macros.REQUEST_SCHEDULING_DEPT_IP)
			{
				if(!machineList.contains(packet.getAddress()))
				{
					System.out.println("Newly joined: "+packet.getAddress());
					machineList.add(packet.getAddress(), packet.getPort());
				}
				//create outbound packet with HELLO message
				final ByteArrayOutputStream baos=new ByteArrayOutputStream();
				final DataOutputStream daos=new DataOutputStream(baos);
				daos.writeInt(Macros.REPLY_SCHEDULING_DEPT_IP);
				daos.close();
				final byte[] bufOut=baos.toByteArray();
				DatagramPacket packetOut = new DatagramPacket(bufOut, bufOut.length, packet.getAddress(), packet.getPort());
				socket.send(packetOut);
			}
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	}