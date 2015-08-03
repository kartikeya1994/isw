package org.isw.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import org.isw.Macros;

public class MachineStage extends Stage{
	Scene realTimeScene;
	Scene resultScene;
	TextArea statusConsole;
	StringProperty statusP = new SimpleStringProperty("STATUS: IDLE");
	public MachineStage(){
		Label statusField = new Label();
		statusField.textProperty().bindBidirectional(statusP);
		statusConsole = new TextArea();
		statusConsole.setWrapText(true);
		statusConsole.setPrefColumnCount(150);
		statusConsole.setPrefRowCount(100);
		GridPane pane = new GridPane();
		pane.setVgap(10);
		pane.setHgap(10);
		pane.setPadding(new Insets(20));
		//pane.add(statusLabel, 0,0);
		pane.add(statusField, 0,0);
		pane.add(statusConsole, 0, 1);
		realTimeScene = new Scene(pane,500,300);
		this.setScene(realTimeScene);
	}
	
	public void appendLog(String s){
		if(s != null && !s.equals("")){
			statusConsole.appendText(s+"\n");
		}
	}
	
	public void setStatus(int status){
		statusP.set(getStatusString(status));	
	}

	private String getStatusString(int status) {
		switch(status){
		case Macros.MACHINE_IDLE:
			return "STATUS: IDLE";
		case Macros.MACHINE_CM:
			return "STATUS: UNDERGOING CORRECTIVE MAINTENANCE";
		case Macros.MACHINE_PM:
			return "STATUS: STATUS: UNDERGOING PREVENTIVE MAINTENANCE";
		case Macros.MACHINE_RUNNING_JOB:
			return "STATUS: RUNNING JOB";
		case Macros.MACHINE_WAITING_FOR_CM_LABOUR:
		case Macros.MACHINE_WAITING_FOR_PM_LABOUR:
			return "STATUS: WAITING FOR LABOUR";	
		case Macros.MACHINE_PLANNING:
			return "STATUS: PLANNING";
		}
		return "";
	}

}
