package org.isw.ui;

import org.isw.Macros;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class MachineStage extends Stage{
	Scene realTimeScene;
	Scene resultScene;
	TextArea statusConsole;
	Label statusField;
	public MachineStage(){
		Label statusLabel = new Label("STATUS:");
		statusField = new Label();
		statusConsole = new TextArea();
		statusConsole.setWrapText(true);
		statusConsole.setPrefColumnCount(150);
		statusConsole.setPrefRowCount(100);
		GridPane pane = new GridPane();
		pane.setVgap(10);
		pane.setHgap(10);
		pane.setPadding(new Insets(20));
		pane.add(statusLabel, 0,0);
		pane.add(statusField, 1,0);
		pane.add(statusConsole, 0, 1);
		realTimeScene = new Scene(pane,500,300);
		this.setScene(realTimeScene);
	}
	
	public void appendLog(String s){
		if(s != null && s!= ""){
			statusConsole.appendText(s+"\n");
		}
	}
	
	public void setStatus(int status){
		statusField.setText(getStatusString(status));	
	}

	private String getStatusString(int status) {
		switch(status){
		case Macros.MACHINE_IDLE:
			return "IDLE";
		case Macros.MACHINE_CM:
			return "UNDERGOING CORRECTIVE MAINTENANCE";
		case Macros.MACHINE_PM:
			return "UNDERGOING PREVENTIVE MAINTENANCE";
		case Macros.MACHINE_RUNNING_JOB:
			return "RUNNING JOB";
		case Macros.MACHINE_WAITING_FOR_CM_LABOUR:
		case Macros.MACHINE_WAITING_FOR_PM_LABOUR:
			return "WAITING FOR LABOUR";	
		}
		return "";
	}

}
