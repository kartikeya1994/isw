package org.isw;

import javafx.beans.property.SimpleStringProperty;

public class Result {
	 public SimpleStringProperty fieldLabel;
     public SimpleStringProperty fieldValue;

     public Result(String label, String value){
    	 fieldLabel = new SimpleStringProperty(label);
    	 fieldValue = new SimpleStringProperty(value);
     }
}
