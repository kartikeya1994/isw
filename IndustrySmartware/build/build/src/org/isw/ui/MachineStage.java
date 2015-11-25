package org.isw.ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import org.isw.Macros;
import org.isw.Result;

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
	
	@SuppressWarnings("unchecked")
	public void showResults(ObservableList<Result>	results,String address){
		TableView<Result> resultTable = new TableView<Result>();
		resultTable.setEditable(false);
		TableColumn<Result,String> labelColumn = new TableColumn<Result,String>();
		labelColumn.setCellValueFactory(data->data.getValue().fieldLabel);
		TableColumn<Result,String> valueColumn = new TableColumn<Result,String>();
		valueColumn.setCellValueFactory(data->data.getValue().fieldValue);
		resultTable.getColumns().addAll(labelColumn,valueColumn);
		resultTable.setPadding(new Insets(20));
		resultTable.setItems(results);
		resultScene = new Scene(resultTable,400,500);
		this.setScene(resultScene);
		this.setTitle("Results - "+address);
	}
	public void setStatus(int status){
		statusP.set(getStatusString(status));	
	}

	public void showChart(int[] pmJobs, int cmJobs[],String address){
		Stage barchartStage   = new Stage();
		barchartStage.setTitle("Component-wise results - "+address);
		final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        final BarChart<String,Number> bc = new BarChart<String,Number>(xAxis,yAxis);
        xAxis.setLabel("Component");       
        yAxis.setLabel("Jobs");
        XYChart.Series series1 = new XYChart.Series();
        series1.setName("CM jobs");
        for(int i=0;i<cmJobs.length;i++){
        	series1.getData().add(new XYChart.Data(String.valueOf(i+1), cmJobs[i]));
        }
        XYChart.Series series2 = new XYChart.Series();
        series2.setName("PM jobs");
        for(int i=0;i<pmJobs.length;i++){
        	series2.getData().add(new XYChart.Data(String.valueOf(i+1), pmJobs[i]));
        }
        
        bc.getData().addAll(series1, series2);	
        Scene bcScene = new Scene(bc,500,400);
        barchartStage.setScene(bcScene);
        barchartStage.show();
        
  
        

	}
	private String getStatusString(int status) {
		switch(status){
		case Macros.MACHINE_IDLE:
			return "STATUS: IDLE";
		case Macros.MACHINE_CM:
			return "STATUS: UNDERGOING CORRECTIVE MAINTENANCE";
		case Macros.MACHINE_PM:
			return "STATUS: UNDERGOING PREVENTIVE MAINTENANCE";
		case Macros.MACHINE_RUNNING_JOB:
			return "STATUS: RUNNING JOB";
		case Macros.MACHINE_WAITING_FOR_CM_LABOUR:
		case Macros.MACHINE_WAITING_FOR_PM_LABOUR:
			return "STATUS: WAITING FOR LABOUR";	
		case Macros.MACHINE_PLANNING:
			return "STATUS: PLANNING";
		case Macros.MACHINE_REPLANNING:
			return "STATUS: REPLANNING";
		case Macros.MACHINE_PROCESS_COMPLETE:
			return "STATUS: PROCESS COMPLETE";
		}
		return "";
	}

}
