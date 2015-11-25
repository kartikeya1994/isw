package org.isw.threads;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import javafx.application.Platform;

import org.isw.MaintenanceStatusPacket;
import org.isw.ui.MaintenanceDeptStage;

public class MaintenanceLoggingThread implements Runnable{
	
	private MaintenanceDeptStage ms;
	Socket socket;
	public MaintenanceLoggingThread(Socket socket){
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				ms = new MaintenanceDeptStage();
				ms.setTitle("Maintenance Dept");
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
				 MaintenanceStatusPacket msp =	(MaintenanceStatusPacket) in.readObject();
				 Platform.runLater(new Runnable(){
					@Override
					public void run() {
						 ms.appendLog(msp.logMessage);
						 if(msp.labour != null)
							 ms.setLabourStatus(msp.labour);
					}
					 
				 });
				
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		
		
		
	}

}
