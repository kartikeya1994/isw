package org.isw;
	
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.isw.threads.ListenerThread;
import org.isw.threads.LoggingThread;
import org.isw.threads.ServerThread;

import com.aquafx_project.AquaFx;


public class Main extends Application {
	public static Text schedulerStatus;
	public static Text maintenanceStatus;
	public static TilePane machineBox;
	public static HashMap<InetAddress,Component[]> machines;
	public static InetAddress schedulerIP;
	public static InetAddress maintenanceIP;
	Scene scene1,scene2;
	Stage stage;
	DatagramSocket udpSocket;
	public static double[] labourCost;
	private WebSocketServer ws;
	static MachineList machineList;
	@Override
	public void start(Stage primaryStage) {
		ws = new WebSocketServer(9091);
		try {
			parseExcel();
			udpSocket = new DatagramSocket(Macros.ISW_PORT);
			stage = primaryStage;
			scene1 = getScene1();
			scene2 = getScene2();
			primaryStage.setTitle("Industry Smartware App");
			primaryStage.setScene(scene1);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		ServerThread serverThread = new ServerThread(ws);
		serverThread.start();
		machines = new HashMap<InetAddress,Component[]>();
		machineList = new MachineList();
		ListenerThread listener = new ListenerThread(machineList,ws);
		listener.start();
		
	}
	
	private void parseExcel() throws IOException {
		FileInputStream file = new FileInputStream(new File("components.xlsx"));
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		XSSFSheet labourSheet = workbook.getSheetAt(1);		
		Row row = labourSheet.getRow(2);
		labourCost = new double[3];
		labourCost[0] = row.getCell(1).getNumericCellValue();
		labourCost[1] = row.getCell(2).getNumericCellValue();
		labourCost[2] = row.getCell(3).getNumericCellValue();
	}

	private Scene getScene2() {
		
		GridPane pane = new GridPane();
		pane.setHgap(10);
        pane.setVgap(10);
		Label label1 = new Label("Number of days to simulate:");
		TextField days = new TextField ("60");
		pane.add(label1, 0, 0);
		pane.add(days, 1, 0);
		pane.add(new Label("Shift duration"),0 , 1);
		pane.add(new Label("Skilled labourers"), 0, 2);
		pane.add(new Label("Semi-skilled laboureres"), 0, 3);
		pane.add(new Label("Unskilled labourers"),0,4);
		pane.add(new Label("Time scale factor"), 0, 5);
		pane.add(new Label("Simulation count"),0,6);
		TextField shiftDuration = new TextField("1440");
		pane.add(shiftDuration,1,1);
		TextField skilled = new TextField("2");
		TextField semiskilled = new TextField("4");
		TextField unskilled = new TextField("8");
		TextField scaleFactor = new TextField("1");
		TextField simulationCount = new TextField("100");
		pane.add(skilled,1,2);
		pane.add(semiskilled,1,3);
		pane.add(unskilled,1,4);
		pane.add(scaleFactor, 1, 5);
		pane.add(simulationCount, 1, 6);
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
				try {
					new Thread(new LoggingThread()).start();;
					initMachines();
					initMaintenance();
					initScheduler();
					
					Thread.sleep(5);
					startSimulation();
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
						
			}


			private void startSimulation() throws IOException {
				DatagramPacket packet = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.START_SCHEDULING);
				udpSocket.send(packet);
				packet = FlagPacket.makePacket(Macros.MAINTENANCE_DEPT_GROUP, Macros.MAINTENANCE_DEPT_MULTICAST_PORT, Macros.START_MAINTENANCE_PLANNING);
				udpSocket.send(packet);
			}

			private void initScheduler() throws NumberFormatException, IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeInt(Integer.parseInt(shiftDuration.getText()));
				dos.writeInt(Integer.parseInt(days.getText()));
				dos.writeInt(Integer.parseInt(scaleFactor.getText()));
				byte compdata[] = baos.toByteArray();
				DatagramPacket packet = new DatagramPacket(compdata,compdata.length,schedulerIP,Macros.SCHEDULING_DEPT_PORT);
				udpSocket.send(packet);
			}

			private void initMaintenance() throws NumberFormatException, IOException {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos);
				dos.writeInt(Integer.parseInt(shiftDuration.getText()));
				dos.writeInt(Integer.parseInt(scaleFactor.getText()));
				dos.writeInt(Integer.parseInt(simulationCount.getText()));
				dos.writeInt(Integer.parseInt(skilled.getText()));
				dos.writeInt(Integer.parseInt(semiskilled.getText()));
				dos.writeInt(Integer.parseInt(unskilled.getText()));
				byte[] data = baos.toByteArray();
				DatagramPacket packet = new DatagramPacket(data,data.length,maintenanceIP,Macros.MAINTENANCE_DEPT_PORT); 
				udpSocket.send(packet);
			}

			private void initMachines() throws IOException {
				for(Entry<InetAddress, Component[]> entry: machines.entrySet()){
					if(entry.getValue() != null){
							InitConfig ic = new InitConfig();
							ic.compList = entry.getValue();
							ic.scaleFactor = Integer.parseInt(scaleFactor.getText());
							ic.shiftDuration = Integer.parseInt(shiftDuration.getText());
					    	ic.simuLationCount = Integer.parseInt(simulationCount.getText());
							ic.send(entry.getKey(),Macros.MACHINE_PORT_TCP);
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
        machineBox.setVgap(10);
        machineBox.setHgap(10);
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
		//AquaFx.style();
		launch(args);
	}
}
