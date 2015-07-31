package org.isw;
	
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map.Entry;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import org.isw.threads.ListenerThread;


public class Main extends Application {
	public static Text schedulerStatus;
	public static Text maintenanceStatus;
	public static TilePane machineBox;
	public static HashMap<InetAddress,Component[]> machines;
	Scene scene1,scene2;
	Stage stage;
	DatagramSocket udpSocket;
	@Override
	public void start(Stage primaryStage) {
		
		try {
			 udpSocket = new DatagramSocket(Macros.ISW_PORT);
			 stage = primaryStage;
			 scene1 = getScene1();
			 scene2 = getScene2();
			//FlatterFX.style();
			//scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setTitle("Industry Smartware");
			primaryStage.setScene(scene1);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		machines = new HashMap<InetAddress,Component[]>();
		MachineList machineList = new MachineList();
		ListenerThread listener = new ListenerThread(machineList);
		listener.start();
	}
	
	private Scene getScene2() {
		
		GridPane pane = new GridPane();
		pane.setHgap(10);
        pane.setVgap(10);
		Label label1 = new Label("Number of days to simulate:");
		TextField days = new TextField ();
		pane.add(label1, 0, 0);
		pane.add(days, 1, 0);
		pane.add(new Label("Shift duration"),0 , 1);
		TextField shiftDuration = new TextField();
		pane.add(shiftDuration,1,1);
		BorderPane bp = new BorderPane();
		bp.setCenter(pane);
		bp.setPadding(new Insets(20));
		HBox buttons = new HBox(10);
		Button back = new Button("Back");
		Button start = new Button("Start");
		buttons.getChildren().addAll(back, start);
		start.setOnAction(new EventHandler<ActionEvent>(){

			@Override
			public void handle(ActionEvent event) {
				for(Entry<InetAddress, Component[]> entry: machines.entrySet()){
					if(entry.getValue() != null){
						
					      try {
					    	  ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ObjectOutputStream oos = new ObjectOutputStream(baos);
							oos.writeObject(entry.getValue());
							byte[] data = baos.toByteArray();
							oos.close();
							DatagramPacket packet = new DatagramPacket(data,data.length,entry.getKey(),Macros.MACHINE_PORT); 
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
				
			}
			
		});
		back.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				stage.setScene(scene1);
			}
		});
		bp.setBottom(buttons);
		return new Scene(bp,500,300);
	}

	private Scene getScene1() {
		

		GridPane grid = new GridPane();
		grid.setHgap(10);
        grid.setVgap(10);
        machineBox = new TilePane();
        machineBox.setPrefColumns(3);
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.getChildren().addAll(new Label("Machines:"),machineBox);
		schedulerStatus = new Text("offline");
		schedulerStatus.setFill(Color.RED);
		maintenanceStatus = new Text("offline");
		maintenanceStatus.setFill(Color.RED);
		
		grid.add(new Text("Scheduling Dept:"), 0, 0);
		grid.add(new Text("Maintenance Dept:"), 2, 0);
		grid.add(schedulerStatus, 1,0);
		grid.add(maintenanceStatus, 3, 0);
		Button proceed = new Button("Proceed");
		proceed.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent event) {
				stage.setScene(scene2);
				
			}
		});

		BorderPane bp = new BorderPane();
		bp.setPadding(new Insets(20));
		bp.setTop(grid);
		bp.setCenter(vbox);
		bp.setBottom(proceed);
		return new Scene(bp,500,300);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
