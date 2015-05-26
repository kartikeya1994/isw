package org.isw;

import java.io.Serializable;

public class SimulationResult implements Serializable {
  /**
	 * 
	 */
private static final long serialVersionUID = 1L;
  double pmAvgTime;
  int compCombo;
  int pmOpportunity;
  public long t; //to be used for calculations by maintenance dept
  public int id; //to be used for calculations by maintenance dept
  public SimulationResult(double pmAvgTime,int compCombo,int pmOpportunity){
	  this.pmAvgTime = pmAvgTime;
	  this.compCombo = compCombo;
	  this.pmOpportunity = pmOpportunity;
			 
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


}
