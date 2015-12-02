package org.isw.threads;

import java.io.IOException;
import java.io.ObjectInputStream;
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
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				if(ois.readInt() == 1)
					new Thread(new MaintenanceLoggingThread(socket.getInetAddress(),ois)).start();
				else
					new Thread(new MachineLoggingThread(socket.getInetAddress(),ois,lock)).start();
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
