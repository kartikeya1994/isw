package org.isw.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import javafx.application.Platform;
import javafx.scene.paint.Color;

import org.isw.FlagPacket;
import org.isw.MachineList;
import org.isw.Macros;
import org.isw.Main;

public class ListenerThread extends Thread
{
	MachineList machineList;
	public ListenerThread(MachineList machineList)
	{
		this.machineList=machineList;
	}

	public void run()
	{
		MulticastSocket socket;
		int machineCount = 0;
		try
		{
			socket = new MulticastSocket(Macros.ISW_MULTICAST_PORT);
			socket.joinGroup(InetAddress.getByName(Macros.ISW_GROUP));
			while(true)
			{
				FlagPacket fp = FlagPacket.receiveMulticast(socket);
				if((fp.flag & Macros.REQUEST_ISW_IP) != 0){
					if((fp.flag & Macros.MACHINE_FLAG) != 0){
					ClientHandlerThread worker = new ClientHandlerThread(socket, fp, machineList);
					worker.start();
					}
					else if((fp.flag & Macros.SCHEDULING_DEPT_FLAG) !=0 ){
						DatagramPacket packet = FlagPacket.makePacket(fp.ip.getHostAddress(), fp.port, Macros.REPLY_ISW_IP);
						socket.send(packet);
						 Platform.runLater(new Runnable() {
						        @Override
						        public void run() {
						          Main.schedulerStatus.setText("online");
						          Main.schedulerStatus.setFill(Color.GREEN);
						        }
						      });
					}
					else if((fp.flag & Macros.MAINTENANCE_DEPT_FLAG) !=0){
						DatagramPacket packet = FlagPacket.makePacket(fp.ip.getHostAddress(), fp.port, Macros.REPLY_ISW_IP);
						socket.send(packet);
						 Platform.runLater(new Runnable() {
						        @Override
						        public void run() {
						          Main.maintenanceStatus.setText("online");
						          Main.maintenanceStatus.setFill(Color.GREEN);
						        }
						      });
					}
			
				}
				//case Macros.REQUEST_MACHINE_LIST:
				//	System.out.println("Request for machineList");
				//	MachineList.send(machineList, fp.ip, fp.port);
				//case Macros.REQUEST_TIME:
				//	if(machineCount++ == machineList.count()){
				//		machineCount = 0;
				//		DatagramPacket timePacket = FlagPacket.makePacket(fp.ip.getHostAddress(), fp.port, Macros.REPLY_TIME);
				//		socket.send(timePacket);
				//	}
					
			
				
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	
	}
}