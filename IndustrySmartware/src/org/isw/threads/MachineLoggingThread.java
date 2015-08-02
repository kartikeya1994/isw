package org.isw.threads;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import javafx.application.Platform;

import org.isw.MachineStatusPacket;
import org.isw.ui.MachineStage;

public class MachineLoggingThread implements Runnable{
	
	private MachineStage ms;
	Socket socket;
	public MachineLoggingThread(Socket socket){
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				ms = new MachineStage();
				ms.show();
			}
			
		});
		this.socket = socket;
		
	}
	
	@Override
	public void run() {
			try {
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				while(true){
				 MachineStatusPacket msp =	(MachineStatusPacket) in.readObject();
				 Platform.runLater(new Runnable(){
					@Override
					public void run() {
						 ms.appendLog(msp.logMessage);
						 ms.setStatus(msp.status);
					}
					 
				 });
				
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		
		
		
	}

}
