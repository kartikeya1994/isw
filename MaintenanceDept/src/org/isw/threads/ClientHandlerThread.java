package org.isw.threads;

import java.net.DatagramPacket;
import java.net.MulticastSocket;

import org.isw.FlagPacket;
import org.isw.MachineList;
import org.isw.Macros;

class ClientHandlerThread extends Thread {

	MulticastSocket socket;
	FlagPacket packet;
	MachineList machineList;
	public ClientHandlerThread(MulticastSocket socket, FlagPacket packet, MachineList machineList) 
	{
		this.socket=socket;
		this.packet=packet;
		this.machineList=machineList;
	}

	@Override
	public void run() {
		try
		{
			if(!machineList.contains(packet.ip))
			{
				System.out.println("Newly joined: "+packet.ip);
				machineList.add(packet.ip, packet.port);
			}
			
			DatagramPacket packetOut =FlagPacket.makePacket(packet.ip.getHostAddress(), packet.port, Macros.REPLY_MAINTENANCE_DEPT_IP);	
			socket.send(packetOut);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	}