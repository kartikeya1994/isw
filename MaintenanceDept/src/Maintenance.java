import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

import com.mb14.MachineList;
import com.mb14.Macros;


public class Maintenance {

	MachineList list;
	public static void main(String[] args)
	{
		boolean recd_list;
		while(true)
		{
			recd_list = false;
			//broadcasting to request for machine list
			//create outbound packet with HELLO message
			final ByteArrayOutputStream baos=new ByteArrayOutputStream();
			final DataOutputStream dos=new DataOutputStream(baos);
			dos.writeInt(Macros.REQUEST_MACHINE_LIST);
			dos.close();
			final byte[] buf=baos.toByteArray();
			DatagramPacket packetOut, packetIn;
			InetAddress group = InetAddress.getByName(Macros.MAINTENANCE_SCHEDULING_GROUP);
			packetOut = new DatagramPacket(buf, buf.length, group, Macros.SCHEDULING_DEPT_PORT);

			//create socket
			DatagramSocket socket = new DatagramSocket(Macros.MAINT);
			socket.setSoTimeout(3000);

			while(!recd_list)
			{
				System.out.println("Getting machine list...");
				socket.send(packetOut);
				
				try
				{

				}catch(SocketTimeoutException stoe)
				{
					System.out.println("Timed out.");
					continue;
				}

				if( == Macros.REPLY_MACHINE_LIST)
				{
					recd_list=true;
					serverIP=packetIn.getAddress();
					//print recd list
				}
				else
					continue;
			}
		}
	}

}
