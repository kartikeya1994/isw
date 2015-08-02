package org.isw;

import java.io.Serializable;

public class MachineStatusPacket implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public int status;
	public String logMessage;
	public MachineStatusPacket(int status, String logMessage) {
		this.status = status;
		this.logMessage = logMessage;
	}
}
