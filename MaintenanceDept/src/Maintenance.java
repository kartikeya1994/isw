import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import org.isw.FlagPacket;
import org.isw.MachineList;
import org.isw.Macros;


public class Maintenance {

	MachineList list;
	public static void main(String[] args)
	{
		boolean recd_list;
		DatagramSocket udpSocket = null;
		ServerSocket tcpSocket = null;
		recd_list = false;
		//broadcast to receive 

		//create packet
		DatagramPacket packetOut = FlagPacket.makePacket(Macros.MAINTENANCE_SCHEDULING_GROUP, Macros.SCHEDULING_DEPT_PORT,Macros.REQUEST_MACHINE_LIST);

		//create socket
		try
		{
			udpSocket = new DatagramSocket(Macros.MAINTENANCE_DEPT_PORT_TCP);

			tcpSocket = new ServerSocket(Macros.MAINTENANCE_DEPT_PORT_TCP);

			while(!recd_list)
			{
				System.out.println("Requesting machine list from scheduling dept...");
				udpSocket.send(packetOut); //UDP

				MachineList list = MachineList.receive(tcpSocket);

				if( list != null)
				{
					recd_list=true;
					System.out.println("Received machine list from "+ list.senderIP);
					System.out.println(list);
					//print recd list
				}
				else
					continue;
			}
		}catch(Exception e)
		{
			e.printStackTrace();
		}

		udpSocket.close();

		try 
		{
			tcpSocket.close();
		}catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

}
