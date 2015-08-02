package org.isw.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javafx.application.Platform;

import org.isw.Macros;
import org.isw.ui.MachineStage;

public class LoggingThread implements Runnable {

	@Override
	public void run() {
		ServerSocket tcpSocket;
		try {
			tcpSocket = new ServerSocket(Macros.ISW_TCP_PORT);
			while(true){
				
				Socket socket = tcpSocket.accept();
				
				new Thread(new MachineLoggingThread(socket)).start();
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
