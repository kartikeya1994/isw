package org.isw;

public class SimulationResult {
  double cost;
  double pmAvgTime;
  int compCombo;
  int pmOpportunity;
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
