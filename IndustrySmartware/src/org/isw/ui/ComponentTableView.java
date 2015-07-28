package org.isw.ui;



import org.isw.Component;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;


public class ComponentTableView extends VBox {
	public ComponentTableView(){
		TableView<Component> componentView = new TableView<Component>();
		componentView.setEditable(true);
		TableColumn<Component,Boolean> checkColumn = new TableColumn<Component,Boolean>("Select");
		checkColumn.setCellValueFactory((new PropertyValueFactory<Component,Boolean>("active")));
		checkColumn.setCellFactory(CheckBoxTableCell.forTableColumn(checkColumn));

		this.getChildren().add(componentView);
		Button saveButton = new Button("Button");
		saveButton.setOnAction(new EventHandler<ActionEvent>(){

			@Override
			public void handle(ActionEvent event) {
				
			}
			
		});
	}

}
