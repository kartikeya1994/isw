package org.isw;

import java.io.Serializable;

public class MachineResultPacket implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2957449886663943682L;
	
	public double cmCost;
	public double pmCost;
	public long downTime;
	public long waitTime;
	public  int jobsDone;
	public  int cmJobsDone;
	public  int pmJobsDone;
	public  int compCMJobsDone[];
	public  int compPMJobsDone[];
	public  long procCost;
	public  long penaltyCost;
	public long cmDownTime;
	public  long pmDownTime;
	public  long runTime;
	public  long idleTime;
	
}
