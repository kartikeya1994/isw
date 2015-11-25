package org.isw.threads;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.isw.Macros;

public class LoggingThread implements Runnable {

	@Override
	public void run() {
		ServerSocket tcpSocket;
		final Object lock = new Object();
		try {
			tcpSocket = new ServerSocket(Macros.ISW_TCP_PORT);
			while(true){
				
				Socket socket = tcpSocket.accept();
				DataInputStream dis = new DataInputStream(socket.getInputStream());
				if(dis.readInt() == 1)
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
