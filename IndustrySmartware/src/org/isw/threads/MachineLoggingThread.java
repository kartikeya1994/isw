package org.isw.threads;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.isw.MachineResultPacket;
import org.isw.MachineStatusPacket;
import org.isw.Result;
import org.isw.ui.MachineStage;

public class MachineLoggingThread implements Runnable{
	
	private MachineStage ms;
	Socket socket;
	public MachineLoggingThread(Socket socket){
		Platform.runLater(new Runnable(){

			@Override
			public void run() {
				ms = new MachineStage();
				ms.setTitle("Machine: "+socket.getInetAddress().getHostAddress());
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
				 Object o = in.readObject();
				 if(o instanceof MachineStatusPacket){
					 MachineStatusPacket msp = (MachineStatusPacket) o;	 
				 Platform.runLater(new Runnable(){
					@Override
					public void run() {
						 ms.appendLog(msp.logMessage);
						 ms.setStatus(msp.status);
					}
				  
				 });
				 }
				 else if(o instanceof MachineResultPacket){
					 MachineResultPacket mrp = (MachineResultPacket) o;	 
					 Platform.runLater(new Runnable(){
						@Override
						public void run() {
							ObservableList<Result> results = FXCollections.observableArrayList();
							results.add(new Result("CM downtime",String.valueOf(mrp.cmDownTime)+" hours"));
							results.add(new Result("PM downtime",String.valueOf(mrp.pmDownTime)+" hours"));
							results.add(new Result("Waiting time",String.valueOf(mrp.waitTime)+" hours"));
							results.add(new Result("Idle time",String.valueOf(mrp.idleTime)+" hours"));
							results.add(new Result("CM cost",String.valueOf(mrp.cmCost)));
							results.add(new Result("PM cost",String.valueOf(mrp.pmCost)));
							results.add(new Result("Penalty cost",String.valueOf(mrp.penaltyCost)));
							results.add(new Result("Processing cost",String.valueOf(mrp.procCost)));
							results.add(new Result("Jobs processed",String.valueOf(mrp.jobsDone)));
							results.add(new Result("CM jobs",String.valueOf(mrp.cmJobsDone)));
							results.add(new Result("PM jobs",String.valueOf(mrp.pmJobsDone)));
							ms.showResults(results,socket.getInetAddress().getHostAddress());
							ms.showChart(mrp.compPMJobsDone, mrp.compCMJobsDone,socket.getInetAddress().getHostAddress());
						}
					  
					 }); 
				 }
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		
		
		
	}

}
