package org.isw.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.isw.Macros;
import org.isw.Main;

public class LoggingThread implements Runnable {

	@Override
	public void run() {
		ServerSocket tcpSocket;
		final Object lock = new Object();
		try {
			tcpSocket = new ServerSocket(Macros.ISW_TCP_PORT);
			while(true){
				
				Socket socket = tcpSocket.accept();
				if(socket.getInetAddress().equals(Main.maintenanceIP))
					new Thread(new MaintenanceLoggingThread(socket)).start();
				else
					new Thread(new MachineLoggingThread(socket,lock)).start();
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
