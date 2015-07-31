package org.isw;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.Arrays;

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
	
	public static void setStatus(int status)
	{
		machineStatus = status;
	}
	
	public static int getStatus()
	{
		return machineStatus;
	}
	
	public static void main(String[] args) {
		boolean maintenanceRegistered=false;
		boolean iswRegistered = false;
		boolean schedulerRegistered = false;
		Macros.loadMacros();
		
		try
		{
			DatagramPacket schedulerPacket  = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.REQUEST_ISW_IP);
			DatagramPacket iswPacket = FlagPacket.makePacket(Macros.ISW_GROUP, Macros.ISW_MULTICAST_PORT, Macros.MACHINE_FLAG|Macros.REQUEST_ISW_IP);
			DatagramPacket maintenancePacket = FlagPacket.makePacket(Macros.MAINTENANCE_DEPT_GROUP, Macros.MAINTENANCE_DEPT_MULTICAST_PORT, Macros.REQUEST_MAINTENANCE_DEPT_IP);
			FlagPacket packetIn;
			//create socket
			DatagramSocket socket = new DatagramSocket(Macros.MACHINE_PORT);
			tcpSocket = new ServerSocket(Macros.MACHINE_PORT_TCP);
			socket.setSoTimeout(3000);

			while(!schedulerRegistered || !iswRegistered || !maintenanceRegistered) // loop until registered with all
			{
				System.out.println("Finding server...");
				if(!maintenanceRegistered) // register machine with maintenance
					socket.send(schedulerPacket);
				if(!iswRegistered) //register machine with central logging
					socket.send(iswPacket);
				if(!schedulerRegistered) // register machine with scheduler
					socket.send(maintenancePacket);
				
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
					maintenanceRegistered = true;
					break;
				case Macros.REPLY_SCHEDULING_DEPT_IP:
					schedulerIP = packetIn.ip;
					schedulerRegistered = true;
					break;
				case Macros.REPLY_ISW_IP:	
					Logger.init(packetIn.ip);
					iswRegistered = true;
					break;
				}
			}
			boolean init = false;
			while(!init){
				byte[] bufIn = new byte[4096*8];
				DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
				//Receive signals from Scheduling Dept
				try
				{
				socket.receive(packet); 
				}
				catch(SocketTimeoutException stoe) {
					System.out.println("Timed out.");
					continue; 
				}
				byte[] reply=packet.getData();
				byte [] header = Arrays.copyOfRange(reply, 0, 12);
				final ByteArrayInputStream bais=new ByteArrayInputStream(header);
				DataInputStream dias =new DataInputStream(bais);
				Macros.SHIFT_DURATION = dias.readInt();
				Macros.TIME_SCALE_FACTOR = dias.readInt();
				Macros.SIMULATION_COUNT = dias.readInt();
				byte[] data = Arrays.copyOfRange(reply, 12, reply.length);
				ByteArrayInputStream in = new ByteArrayInputStream(data);
				ObjectInputStream is = new ObjectInputStream(in);
				compList = (Component[])is.readObject();
				init = true;
			}
			
			//Variables required for the simulation results
			downTime = 0;
			jobsDone = 0;
			cmJobsDone = pmJobsDone = 0;
			shiftCount = 0;
			cmCost = 0;
			pmCost = 0;
			compCMJobsDone = new int[compList.length];
			compPMJobsDone = new int[compList.length];
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
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}