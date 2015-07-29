package org.isw.ui;



import java.io.File;
import java.io.FileInputStream;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.isw.Component;


public class ComponentTableView extends VBox {
	public ComponentTableView(){
		TableView<Component> componentTable = new TableView<Component>();
		TableColumn<Component,Boolean> checkColumn = new TableColumn<Component,Boolean>("Select");
		checkColumn.setCellValueFactory((new PropertyValueFactory<Component,Boolean>("active")));
		checkColumn.setCellFactory(CheckBoxTableCell.forTableColumn(checkColumn));
		componentTable.getColumns().addAll(checkColumn,createPMColumns(),createCMColumns());
		Button saveButton = new Button("Save");
		saveButton.setOnAction(new EventHandler<ActionEvent>(){

			@Override
			public void handle(ActionEvent event) {
				
			}
			
		});
		this.getChildren().addAll(componentTable,saveButton);
	}

	private TableColumn createPMColumns() {
		TableColumn pm = new TableColumn("Preventive Maintainence");
		TableColumn maintainability = new TableColumn("Maintainabilty");
		TableColumn<Component,Double> pmMuRep = new TableColumn<Component, Double>("mu repair");
		pmMuRep.setCellValueFactory(cellData -> cellData.getValue().pmMuRep);
		TableColumn<Component,Double> pmSigmaRep = new TableColumn<Component, Double>("sigma repair)");
		pmSigmaRep.setCellValueFactory(cellData -> cellData.getValue().pmSigmaRep);
		maintainability.getColumns().addAll(pmMuRep,pmSigmaRep);
		TableColumn supportability = new TableColumn("Supportabilty");
		TableColumn<Component,Double> pmMuSupp = new TableColumn<Component, Double>("mu support");
		pmMuSupp.setCellValueFactory(cellData -> cellData.getValue().pmMuSupp);
		TableColumn<Component,Double> pmSigmaSupp = new TableColumn<Component, Double>("sigma support");
		pmSigmaSupp.setCellValueFactory(cellData -> cellData.getValue().pmSigmaSupp);
		maintainability.getColumns().addAll(pmMuSupp,pmSigmaSupp);	
		TableColumn<Component,Double> rf = new TableColumn<Component, Double>("Restoration Factor");
		rf.setCellValueFactory(cellData -> cellData.getValue().pmRF);
		TableColumn cost = new TableColumn("Cost Models");
		TableColumn<Component,Double> sparePartCost = new TableColumn<Component, Double>("Spare parts");
		sparePartCost.setCellValueFactory(cellData -> cellData.getValue().pmCostSpare);
		TableColumn<Component,Double> otherCost = new TableColumn<Component, Double>("Other");
		otherCost.setCellValueFactory(cellData -> cellData.getValue().pmCostOther);
		cost.getColumns().addAll(sparePartCost,otherCost);
		pm.getColumns().addAll(maintainability,supportability,rf,cost);
		return pm;
	}
	private TableColumn createCMColumns() {
		TableColumn cm = new TableColumn("Corrective Maintainence");
		TableColumn reliability = new TableColumn("Reliability");
		TableColumn<Component,Double> cmEta = new TableColumn<Component, Double>("eta (Hr)");
		TableColumn<Component,Double> cmBeta = new TableColumn<Component, Double>("beta (Hr)");
		reliability.getColumns().addAll(cmEta,cmBeta);
		TableColumn maintainability = new TableColumn("Maintainabilty");
		TableColumn<Component,Double> cmMuRep = new TableColumn<Component, Double>("mu repair");
		TableColumn<Component,Double> cmSigmaRep = new TableColumn<Component, Double>("sigma repair)");
		maintainability.getColumns().addAll(cmMuRep,cmSigmaRep);
		TableColumn supportability = new TableColumn("Supportabilty");
		TableColumn<Component,Double> cmMuSupp = new TableColumn<Component, Double>("mu support");
		TableColumn<Component,Double> cmSigmaSupp = new TableColumn<Component, Double>("sigma support");
		maintainability.getColumns().addAll(cmMuSupp,cmSigmaSupp);	
		TableColumn<Component,Double> rf = new TableColumn<Component, Double>("Restoration Factor");
		TableColumn cost = new TableColumn("Cost Models");
		TableColumn<Component,Double> sparePartCost = new TableColumn<Component, Double>("Spare parts");
		TableColumn<Component,Double> otherCost = new TableColumn<Component, Double>("Other");
		cost.getColumns().addAll(sparePartCost,otherCost);
		cm.getColumns().addAll(reliability,maintainability,supportability,rf,cost);
		return cm;
	}
	
	private static Component[] parseExcel(int n) {
		/**
		 * Parse the component excel file into a list of components.
		 * Total number of components should be 14 for our experiment.
		 * Different component excel file for different machineNo (Stick
		 * to one for now)
		 * **/
		Component[] c = new Component[n];
		try
		{
			FileInputStream file = new FileInputStream(new File("Components.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			XSSFSheet labourSheet = workbook.getSheetAt(1);
			for(int i=5;i<5+n;i++)
			{
				Row row = sheet.getRow(i);
				Component comp = new Component();
				//--------CM data------------
				//0 is assembly name
				comp.compName = row.getCell(1).getStringCellValue();
				comp.initAge = row.getCell(2).getNumericCellValue();
				comp.cmEta = row.getCell(3).getNumericCellValue();
				comp.cmBeta = row.getCell(4).getNumericCellValue();
				comp.cmMuRep = row.getCell(5).getNumericCellValue();
				comp.cmSigmaRep = row.getCell(6).getNumericCellValue();
				comp.cmMuSupp = row.getCell(7).getNumericCellValue();
				comp.cmSigmaRep = row.getCell(8).getNumericCellValue();
				comp.cmRF = row.getCell(9).getNumericCellValue();
				comp.cmCostSpare = row.getCell(10).getNumericCellValue();
				comp.cmCostOther = row.getCell(11).getNumericCellValue();
				//12 is empty
				//13 is empty
				
				//--------PM data------------
				//14 is assembly name
				//15 is component name
				//16 is init age
				comp.pmMuRep = row.getCell(17).getNumericCellValue();
				comp.pmSigmaRep = row.getCell(18).getNumericCellValue();
				comp.pmMuSupp = row.getCell(19).getNumericCellValue();
				comp.pmSigmaSupp = row.getCell(20).getNumericCellValue();
				comp.pmRF = row.getCell(21).getNumericCellValue();
				comp.pmCostSpare = row.getCell(22).getNumericCellValue();
				comp.pmCostOther = row.getCell(23).getNumericCellValue();
				row = labourSheet.getRow(i);
				comp.pmLabour = new int[3];
				comp.pmLabour[0] = (int)row.getCell(3).getNumericCellValue();
				comp.pmLabour[1] = (int)row.getCell(5).getNumericCellValue();
				comp.pmLabour[2] = (int)row.getCell(7).getNumericCellValue();
				comp.cmLabour = new int[3];
				comp.cmLabour[0] = (int)row.getCell(2).getNumericCellValue();
				comp.cmLabour[1] = (int)row.getCell(4).getNumericCellValue();
				comp.cmLabour[2] = (int)row.getCell(6).getNumericCellValue();
				c[i-5] = comp;
			
			}
			file.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return c;
	}

}
