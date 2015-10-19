
package org.isw.ui;




import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.ArrayList;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.isw.Component;
import org.isw.Main;


public class ComponentTableView extends VBox {
	@SuppressWarnings("unchecked")
	public ComponentTableView(InetAddress ip){
		super(10);
		CheckBox isMachineSelected = new CheckBox("Select Machine");
		isMachineSelected.setSelected(true);
		TableView<Component> componentTable = new TableView<Component>();
		TableColumn<Component,Boolean> checkColumn = new TableColumn<Component,Boolean>("Select");
		checkColumn.setCellValueFactory(cellData -> cellData.getValue().active);
		checkColumn.setCellFactory(tc -> new CheckBoxTableCell<>());
		checkColumn.setEditable(true);
		TableColumn<Component,String> compNameColumn = new TableColumn<Component,String>(); 
		compNameColumn.setCellValueFactory(cellData -> cellData.getValue().compNameP);
		TableColumn<Component,Number> initAge = new TableColumn<Component,Number>();
		initAge.setCellValueFactory(cellData -> cellData.getValue().initAgeP);
		componentTable.getColumns().addAll(checkColumn,compNameColumn,initAge,createPMColumns(),createCMColumns());
		Button saveButton = new Button("Save");
		componentTable.setEditable(true);
		Component[] components = parseExcel() ;
		//Fix boolean thingy
		saveButton.setOnAction(new EventHandler<ActionEvent>(){

			@Override
			public void handle(ActionEvent event) {
				if(isMachineSelected.isSelected()){
				ArrayList<Component> components = new ArrayList<Component>();
					for(Component comp : componentTable.getItems()){
						if(comp.active.get())
							components.add(comp);
					}
					Component[] comps = new Component[components.size()];
					for (int i = 0; i < comps.length; i++) {
					   comps[i] = components.get(i);
					}
 					Main.machines.put(ip, comps);
				}
				else{
					Main.machines.put(ip, null);
				}
				Node  source = (Node) event.getSource(); 
				Stage window = (Stage)source.getScene().getWindow();
				window.close();
			}
			
		});
		ObservableList<Component> componentData = FXCollections.observableArrayList();
		componentData.addAll(components);
		componentTable.setItems(componentData);
		this.setPadding(new Insets(20));
		this.getChildren().addAll(componentTable,isMachineSelected,saveButton);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private TableColumn createPMColumns() {
		TableColumn pm = new TableColumn("Preventive Maintainence");
		TableColumn maintainability = new TableColumn("Maintainabilty");
		TableColumn<Component,Number> pmMuRep = new TableColumn<Component, Number>("mu repair");
		pmMuRep.setCellValueFactory(cellData -> cellData.getValue().pmMuRepP);
		TableColumn<Component,Number> pmSigmaRep = new TableColumn<Component, Number>("sigma repair)");
		pmSigmaRep.setCellValueFactory(cellData -> cellData.getValue().pmSigmaRepP);
		maintainability.getColumns().addAll(pmMuRep,pmSigmaRep);
		TableColumn supportability = new TableColumn("Supportabilty");
		TableColumn<Component,Number> pmMuSupp = new TableColumn<Component, Number>("mu support");
		pmMuSupp.setCellValueFactory(cellData -> cellData.getValue().pmMuSuppP);
		TableColumn<Component,Number> pmSigmaSupp = new TableColumn<Component, Number>("sigma support");
		pmSigmaSupp.setCellValueFactory(cellData -> cellData.getValue().pmSigmaSuppP);
		supportability.getColumns().addAll(pmMuSupp,pmSigmaSupp);	
		TableColumn<Component,Number> rf = new TableColumn<Component, Number>("Restoration Factor");
		rf.setCellValueFactory(cellData -> cellData.getValue().pmRFP);
		TableColumn cost = new TableColumn("Cost Models");
		TableColumn<Component,Number> sparePartCost = new TableColumn<Component, Number>("Spare parts");
		sparePartCost.setCellValueFactory(cellData -> cellData.getValue().pmCostSpareP);
		TableColumn<Component,Number> otherCost = new TableColumn<Component, Number>("Other");
		otherCost.setCellValueFactory(cellData -> cellData.getValue().pmCostOtherP);
		cost.getColumns().addAll(sparePartCost,otherCost);
		pm.getColumns().addAll(maintainability,supportability,rf,cost);
		return pm;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private TableColumn createCMColumns() {
		TableColumn cm = new TableColumn("Corrective Maintainence");
		TableColumn reliability = new TableColumn("Reliability");
		TableColumn<Component,Number> cmEta = new TableColumn<Component, Number>("eta (Hr)");
		cmEta.setCellValueFactory(cellData -> cellData.getValue().cmEtaP);
		TableColumn<Component,Number> cmBeta = new TableColumn<Component, Number>("beta (Hr)");
		cmBeta.setCellValueFactory(cellData -> cellData.getValue().cmBetaP);
		reliability.getColumns().addAll(cmEta,cmBeta);
		TableColumn maintainability = new TableColumn("Maintainabilty");
		TableColumn<Component,Number> cmMuRep = new TableColumn<Component, Number>("mu repair");
		cmMuRep.setCellValueFactory(cellData -> cellData.getValue().cmMuRepP);
		TableColumn<Component,Number> cmSigmaRep = new TableColumn<Component, Number>("sigma repair)");
		cmSigmaRep.setCellValueFactory(cellData -> cellData.getValue().pmSigmaRepP);
		maintainability.getColumns().addAll(cmMuRep,cmSigmaRep);
		TableColumn supportability = new TableColumn("Supportabilty");
		TableColumn<Component,Number> cmMuSupp = new TableColumn<Component, Number>("mu support");
		cmMuSupp.setCellValueFactory(cellData -> cellData.getValue().cmMuSuppP);
		TableColumn<Component,Number> cmSigmaSupp = new TableColumn<Component, Number>("sigma support");
		cmSigmaSupp.setCellValueFactory(cellData -> cellData.getValue().cmSigmaSuppP);
		supportability.getColumns().addAll(cmMuSupp,cmSigmaSupp);	
		TableColumn<Component,Number> rf = new TableColumn<Component, Number>("Restoration Factor");
		rf.setCellValueFactory(cellData -> cellData.getValue().cmRFP);
		TableColumn cost = new TableColumn("Cost Models");
		TableColumn<Component,Number> sparePartCost = new TableColumn<Component, Number>("Spare parts");
		sparePartCost.setCellValueFactory(cellData -> cellData.getValue().cmCostSpareP);
		TableColumn<Component,Number> otherCost = new TableColumn<Component, Number>("Other");
		otherCost.setCellValueFactory(cellData -> cellData.getValue().cmCostOtherP);
		cost.getColumns().addAll(sparePartCost,otherCost);
		cm.getColumns().addAll(reliability,maintainability,supportability,rf,cost);
		return cm;
	}
	
	private static Component[] parseExcel() {
		/**
		 * Parse the component excel file into a list of components.
		 * Total number of components should be 24 for our experiment.
		 * Different component excel file for different machineNo (Stick
		 * to one for now)
		 * 
		 * */
		Component[] c = new Component[28];
		try
		{
			FileInputStream file = new FileInputStream(new File("components.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			for(int i=5;i<33;i++)
			{
				Row row = sheet.getRow(i);
				Component comp = new Component();
				comp.labourCost = Main.labourCost;
				//--------CM data------------
				//0 is assembly name
				comp.compName = row.getCell(1).getStringCellValue();
				comp.initAge = row.getCell(2).getNumericCellValue();
				comp.cmEta = row.getCell(3).getNumericCellValue();
				comp.cmBeta = row.getCell(4).getNumericCellValue();
				comp.cmMuRep = row.getCell(5).getNumericCellValue();
				comp.cmSigmaRep = row.getCell(6).getNumericCellValue();
				comp.cmMuSupp = row.getCell(7).getNumericCellValue();
				comp.cmSigmaSupp = row.getCell(8).getNumericCellValue();
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
				comp.labourCost = new double[]{500,0,0};
				comp.pmLabour = new int[]{1,0,0};
				comp.cmLabour = new int[]{1,0,0};

				comp.initProps(i-5);
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
