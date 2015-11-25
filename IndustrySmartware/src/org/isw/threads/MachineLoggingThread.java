package org.isw.threads;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
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
	Object lock; 
	public MachineLoggingThread(Socket socket,Object lock){
		this.lock = lock;
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
			while(true)
			{
				Object o = in.readObject();
				if(o instanceof MachineStatusPacket)
				{
					MachineStatusPacket msp = (MachineStatusPacket) o;	 
					Platform.runLater(new Runnable(){
						@Override
						public void run() {
							ms.appendLog(msp.logMessage);
							ms.setStatus(msp.status);
						}

					});
				}
				else if(o instanceof MachineResultPacket)
				{
					Platform.runLater(new Runnable(){
						@Override
						public void run() 
						{
							MachineResultPacket mrp = (MachineResultPacket) o;	 
							synchronized(lock){
								try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("results.csv",true))))
								{ 
									for(int i=0;i<mrp.compNames.length;i++)
										out.format("%s;", mrp.compNames[i]);
									out.print(",");
									out.format("%f,", mrp.planningTime);
									out.format("%f,", mrp.availabiltiy);
									out.format("%d,",  mrp.idleTime);
									out.format("%d,", mrp.pmDownTime);
									out.format("%d,",  mrp.cmDownTime);
									out.format("%d,", mrp.waitTime);
									out.format("%f,",  mrp.pmCost);
									out.format("%f,", mrp.cmCost);
									out.format("%d,",  mrp.penaltyCost);
									out.print(" ,");
									out.format("%d,",  mrp.procCost);
									out.print(" ,");
									out.format("%f,",  mrp.jobsDone);
									out.format("%f,", mrp.pmJobsDone);
									out.format("%f\n",  mrp.cmJobsDone);
								}
								catch(IOException e){
									e.printStackTrace();
								}
							}
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
					// Process Complete
					return;
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}



	}

}
