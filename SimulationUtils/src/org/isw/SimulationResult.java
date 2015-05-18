package org.isw;

import java.io.Serializable;

public class SimulationResult  implements Serializable {
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
  double cost;
  double pmAvgTime;
  int compCombo;
  int pmOpportunity;
  public long t; //to be used for calculations by maintenance dept
  public int id; //to be used for calculations by maintenance dept
  public SimulationResult(double cost,double pmAvgTime,int compCombo,int pmOpportunity){
	  this.cost = cost;
	  this.pmAvgTime = pmAvgTime;
	  this.compCombo = compCombo;
	  this.pmOpportunity = pmOpportunity;
			 
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
}
