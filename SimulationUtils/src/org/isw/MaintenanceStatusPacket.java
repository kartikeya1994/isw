package org.isw;

import java.io.Serializable;

public class MaintenanceStatusPacket implements Serializable{

	private static final long serialVersionUID = -5500013537223531195L;

	public int labour[];
	public String logMessage;


	public MaintenanceStatusPacket(int[] labour, String logMessage) {
		this.labour = labour;
		this.logMessage = logMessage;
	}
}
