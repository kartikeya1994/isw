package org.isw;

import java.io.File;
import java.io.FileInputStream;

import com.sun.rowset.internal.Row;


public class Machine {

	int machineStatus = Macros.MACHINE_IDLE;
	public int machineNo;
	public  int shiftCount;
	public  Component[] compList;
	public  double cmCost;
	public double pmCost;
	public	 long downTime;
	public long waitTime;
	public int jobsDone;
	public  int cmJobsDone;
	public  int pmJobsDone;
	public  int compCMJobsDone[];
	public  int compPMJobsDone[];
	public  long procCost;
	public  long penaltyCost;
	public  long cmDownTime;
	public  long pmDownTime;
	public  long runTime;
	public  long idleTime;

	
	public Machine(int machineNo, Component[] compList){
		this.compList = compList;
		downTime = 0;
		jobsDone = 0;
		cmJobsDone = pmJobsDone = 0;
		shiftCount = 0;
		cmCost = 0;
		pmCost = 0;
		compCMJobsDone = new int[compList.length];
		compPMJobsDone = new int[compList.length];
		cmDownTime=0;
		pmDownTime=0;
		waitTime=0;
		penaltyCost=0;
		procCost=0;
		runTime =0;
		idleTime = 0;		
		this.machineNo = machineNo;
	}
	
	
	public void setStatus(int status)
	{
		machineStatus = status;
	}
	
	public int getStatus()
	{
		return machineStatus;
	}
	

}