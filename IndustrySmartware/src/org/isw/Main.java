package org.isw;
	
import org.isw.threads.ListenerThread;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import com.guigarage.flatterfx.FlatterFX;


public class Main extends Application {
	public static Text schedulerStatus;
	public static Text maintenanceStatus;
	public static HBox machineBox;
	@Override
	public void start(Stage primaryStage) {
		
		try { 
			VBox vbox = new VBox();			
			GridPane grid = new GridPane();
			grid.setPadding(new Insets(20));
			grid.setHgap(10);
	        grid.setVgap(10);
	        machineBox = new HBox(10);
	        machineBox.setPadding(new Insets(20));
			schedulerStatus = new Text("offline");
			schedulerStatus.setFill(Color.RED);
			maintenanceStatus = new Text("offline");
			maintenanceStatus.setFill(Color.RED);
			ColumnConstraints column1 = new ColumnConstraints();
			column1.setPercentWidth(30);
			ColumnConstraints column2 = new ColumnConstraints();
			column1.setPercentWidth(20);
			grid.getColumnConstraints().addAll(column1,column2,column1,column2);
			grid.add(new Text("Scheduling Dept:"), 0, 0);
			grid.add(new Text("Maintenance Dept:"), 2, 0);
			grid.add(schedulerStatus, 1,0);
			grid.add(maintenanceStatus, 3, 0);
			vbox.getChildren().add(grid);
			vbox.getChildren().add(machineBox);
			
			Scene scene = new Scene(vbox,800,600);
			//FlatterFX.style();
			//scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setTitle("Industry Smartware");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		MachineList machineList = new MachineList();
		ListenerThread listener = new ListenerThread(machineList);
		listener.start();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
