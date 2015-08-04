package org.isw.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class MaintenanceDeptStage extends Stage{
	Scene realTimeScene;
	Scene resultScene;
	TextArea statusConsole;
	StringProperty labourStatusP = new SimpleStringProperty("Labour Available:");
	public MaintenanceDeptStage(){
		Label statusField = new Label();
		statusField.setPrefHeight(80);
		statusField.textProperty().bindBidirectional(labourStatusP);
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
		realTimeScene = new Scene(pane,600,400);
		this.setScene(realTimeScene);
	}
	
	public void appendLog(String s){
		if(s != null && !s.equals("")){
			statusConsole.appendText(s+"\n");
		}
	}
	
	public void setLabourStatus(int[] labour){
		labourStatusP.set(String.format("Labour Available:\tSkilled: %d\tSemi-skilled: %d\tUnskilled: %d", labour[0],labour[1],labour[2]));	
	}

}
