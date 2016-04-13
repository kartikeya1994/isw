package org.isw;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import org.isw.threads.ListenerThread;

public class Machine
{
	static int machineStatus = Macros.MACHINE_IDLE;

	static InetAddress schedulerIP;
	static InetAddress maintenanceIP;
	public static ServerSocket tcpSocket;

	static int machineNo;
	public static int shiftCount;
	public static Component[] compList;
	public static double cmCost;
	public static double pmCost;
	public	static long downTime;
	public static long waitTime;
	public static int jobsDone;
	public static int cmJobsDone;
	public static int pmJobsDone;
	public static int compCMJobsDone[];
	public static int compPMJobsDone[];
	public static long procCost;
	public static long penaltyCost;
	public static long cmDownTime;
	public static long pmDownTime;
	public static long runTime;
	public static long idleTime;
	static int oldStatus = 0;

	public static void setStatus(int status)
	{
		machineStatus = status;
	}

	public static int getStatus()
	{
		return machineStatus;
	}

	public static void main(String[] args) {
		if(args.length > 0){
			if(args[0].equals("BF"))
			{
				Macros.BF = true;
			}
			if(args[0].equals("MA"))
			{
				Macros.BF = false;
			}
			if(args[0].equals("NPM"))
				Macros.NPM = true;
		}
		boolean maintenanceRegistered=false;
		boolean iswRegistered = false;
		boolean schedulerRegistered = false;

		try
		{
			DatagramPacket schedulerPacket  = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.REQUEST_SCHEDULING_DEPT_IP);
			DatagramPacket iswPacket = FlagPacket.makePacket(Macros.ISW_GROUP, Macros.ISW_MULTICAST_PORT, Macros.MACHINE_FLAG|Macros.REQUEST_ISW_IP);
			DatagramPacket maintenancePacket = FlagPacket.makePacket(Macros.MAINTENANCE_DEPT_GROUP, Macros.MAINTENANCE_DEPT_MULTICAST_PORT, Macros.REQUEST_MAINTENANCE_DEPT_IP);
			FlagPacket packetIn;
			//create socket
			DatagramSocket socket = new DatagramSocket(Macros.MACHINE_PORT);
			tcpSocket = new ServerSocket(Macros.MACHINE_PORT_TCP);
			socket.setSoTimeout(3000);

			while(!schedulerRegistered || !iswRegistered || !maintenanceRegistered) // loop until registered with all
			{
				if(!maintenanceRegistered) // register machine with maintenance
					socket.send(schedulerPacket);
				if(!iswRegistered) //register machine with central logging
					socket.send(iswPacket);
				if(!schedulerRegistered) // register machine with scheduler
					socket.send(maintenancePacket);

				System.out.println("Finding server...");
				try
				{
					packetIn = FlagPacket.receiveUDP(socket);//blocking call for 1000ms
				}catch(SocketTimeoutException stoe)
				{
					System.out.println("Timed out.");
					continue;
				}

				switch (packetIn.flag){
				case Macros.REPLY_MAINTENANCE_DEPT_IP:
					maintenanceIP = packetIn.ip;
					System.out.println("Registered to maintenance: "+ maintenanceIP.getHostAddress());
					maintenanceRegistered = true;
					break;
				case Macros.REPLY_SCHEDULING_DEPT_IP:
					schedulerIP = packetIn.ip;
					System.out.println("Registered to scheduler: "+ schedulerIP.getHostAddress());
					schedulerRegistered = true;
					break;
				case Macros.REPLY_ISW_IP:
					Logger.init(packetIn.ip);
					System.out.println("Registered to isw: "+ packetIn.ip.getHostAddress());
					iswRegistered = true;
					break;
				}
			}


			//Variables required for the simulation results
			downTime = 0;
			jobsDone = 0;
			cmJobsDone = pmJobsDone = 0;
			shiftCount = 0;
			cmCost = 0;
			pmCost = 0;
			cmDownTime=0;
			pmDownTime=0;
			waitTime=0;
			penaltyCost=0;
			procCost=0;
			runTime =0;
			idleTime = 0;
			ListenerThread listener = new ListenerThread(schedulerIP, maintenanceIP, socket, tcpSocket);
			listener.start();


		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public static void setOldStatus(int status) {
		oldStatus = status;

	}

	public static int getOldStatus() {
		return oldStatus;
	}

}
