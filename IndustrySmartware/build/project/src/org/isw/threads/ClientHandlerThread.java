package org.isw.threads;

import java.net.DatagramPacket;
import java.net.MulticastSocket;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import org.isw.FlagPacket;
import org.isw.MachineList;
import org.isw.Macros;
import org.isw.Main;
import org.isw.WebSocketServer;
import org.isw.ui.ComponentTableView;
import org.json.simple.JSONObject;

class ClientHandlerThread extends Thread {

	MulticastSocket socket;
	FlagPacket packet;
	MachineList machineList;
	WebSocketServer ws;
	public ClientHandlerThread(MulticastSocket socket, FlagPacket packet, MachineList machineList, WebSocketServer ws) 
	{
		this.socket=socket;
		this.packet=packet;
		this.machineList=machineList;
		this.ws = ws;
	}

	@Override
	public void run() {
		synchronized(machineList){
		try
		{
		
			if(!machineList.contains(packet.ip))
			{
				System.out.println("Newly joined: "+packet.ip);
				machineList.add(packet.ip, packet.port);
				JSONObject obj = new JSONObject();
				obj.put("type","machine");
				obj.put("ip", packet.ip.getHostAddress());
				ws.getWebSocket().send(obj.toJSONString());
				Main.machines.put(packet.ip,null);
				Platform.runLater(new Runnable(){

					@Override
					public void run() {
						Button machine = new Button(packet.ip.getHostAddress());
						machine.setOnAction(new EventHandler<ActionEvent>(){

							@Override
							public void handle(ActionEvent event) {
								// TODO Auto-generated method stub
								 Stage stage = new Stage();
						         stage.setTitle("Select components:");
						         stage.setScene(new Scene(new ComponentTableView(packet.ip), 1200, 600));
						         stage.show();
							}
							
						});
						Main.machineBox.getChildren().add(machine);
					}
					
				});
			}
			
			DatagramPacket packetOut =FlagPacket.makePacket(packet.ip.getHostAddress(), packet.port, Macros.REPLY_ISW_IP);	
			socket.send(packetOut);
			
		}catch(Exception e)
		{
			e.printStackTrace();
		}
	}
		}
	}