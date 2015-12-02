package org.isw;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Logger {
	static InetAddress ip;
	static Socket socket;
	static ObjectOutputStream out;
	public static void init(InetAddress IP){
		ip = IP;
	}
	
	public static void connect(int flag) throws IOException {
		socket = new Socket(ip, Macros.ISW_TCP_PORT);
		out = new ObjectOutputStream(socket.getOutputStream());
		out.writeInt(flag);
	}
	
	public static void log(int status, String logMessage){
	try {
			out.writeObject(new MachineStatusPacket(status,logMessage));		
		} catch (IOException e) {
			
			e.printStackTrace();
		}
	}
	public static void log(int[] labour, String logMessage){
		try {
				out.writeObject(new MaintenanceStatusPacket(labour,logMessage));		
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}

	public static void log(MachineResultPacket mrp) {
		try {
			out.writeObject(mrp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
		
	
	
}
