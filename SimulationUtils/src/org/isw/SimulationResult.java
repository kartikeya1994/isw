package org.isw;

import java.io.Serializable;

public class SimulationResult implements Serializable {
  /**
	 * 
	 */
  private static final long serialVersionUID = 1L;
  public double cost;
  public int[] pmLabour;
  public double pmAvgTime;
  public int compCombo;
  public int pmOpportunity;
  public long t; //to be used for calculations by maintenance dept
  public int id; //to be used for calculations by maintenance dept
  public SimulationResult(double cost,double pmAvgTime,int compCombo,int pmOpportunity,int labour[]){
	  this.cost = cost;
	  this.pmAvgTime = pmAvgTime;
	  this.compCombo = compCombo;
	  this.pmOpportunity = pmOpportunity;
	  this.pmLabour = new int[3];
	  System.arraycopy( labour, 0, pmLabour, 0, labour.length );
  }
  public double getCost(){
	  return cost;
  }
  public double getPMAvgTime(){
	  return pmAvgTime;
  }
  public int getCompCombo(){
	  return compCombo;
  }
  public int getPMOpportunity(){
	  return pmOpportunity;
  }
public void setCost(double cost) {
	this.cost = cost;
	
}
public int pmLabourCount() {
	return pmLabour[0]+pmLabour[1]+pmLabour[2];
}



}
