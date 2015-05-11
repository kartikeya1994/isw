package com.mb14.threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

import com.mb14.MachineList;

class ClientHandlerThread extends Thread {
	final static int HELLO = 1; //not yet registered
	final static int SERVER_ACCEPT = 2; //registered

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
			if(dais.readInt()==HELLO)
			{
				if(!machineList.contains(packet.getAddress()))
				{
					System.out.println("Newly joined: "+packet.getAddress());
					machineList.add(packet.getAddress(), packet.getPort());
				}
				//create outbound packet with HELLO message
				final ByteArrayOutputStream baos=new ByteArrayOutputStream();
				final DataOutputStream daos=new DataOutputStream(baos);
				daos.writeInt(SERVER_ACCEPT);
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