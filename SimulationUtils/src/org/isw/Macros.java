package org.isw;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketAddress;
import java.net.SocketImpl;
import java.util.Properties;


/*
 * Scheduling listens on 8888
 * Machine listens on 8889
 */

public class Macros {
	public static final int REQUEST_SCHEDULING_DEPT_IP = 1;
	public static final int REPLY_SCHEDULING_DEPT_IP = 2;
	public static final int REQUEST_MAINTENANCE_IP = 3;
	public static final int REPLY_MAINTENANCE_IP = 4;
	public static final int REQUEST_MACHINE_LIST = 5;
	public static final int REPLY_MACHINE_LIST = 6;
	public static final int REQUEST_NEXT_SHIFT = 8;
	public static final int REPLY_NEXT_SHIFT = 9;
	public static final int REQUEST_PREVIOUS_SHIFT = 10;
	public static final int MAINTENANCE_DEPT_IP = 7;
	public static final int PROCESS_COMPLETE = 11;
	public static final int REQUEST_TIME = 12;
	public static final int REPLY_TIME = 13;
	public static final int REPLY_ISW_IP = 14;
	public static final int REQUEST_ISW_IP = 15;
	public static final int MACHINE_FLAG = 16;
	public static final int SCHEDULING_DEPT_FLAG = 32;
	public static final int MAINTENANCE_DEPT_FLAG = 64;
	public static final int REQUEST_MAINTENANCE_DEPT_IP = 17;
	public static final int REPLY_MAINTENANCE_DEPT_IP = 18;
	public static final int START_MAINTENANCE_PLANNING = 19;
	public static final int REQUEST_MAINTENANCE = 20;
	public static final int MAINTENANCE_UNAVAILABLE = 21;
	public static final int MAINTENANCE_AVAILABLE = 22;
	public static final int LABOUR_GRANTED = 23;
	public static final int LABOUR_DENIED = 24;
	public static final int MACHINE_IDLE = 25;
	public static final int MACHINE_RUNNING_JOB = 26;
	public static final int MACHINE_WAITING_FOR_PM_LABOUR = 27;
	public static final int MACHINE_WAITING_FOR_CM_LABOUR = 28;
	public static final int MACHINE_PM = 29;
	public static final int MACHINE_CM = 30;
	public static final int START_SCHEDULING = 31;
	public static final int INIT = 32;
	
	public static final int MAINTENANCE_DEPT_MULTICAST_PORT = 8886;
	public static final int ISW_MULTICAST_PORT = 8887;
	public static final int SCHEDULING_DEPT_MULTICAST_PORT = 8888;
	public static final int SCHEDULING_DEPT_PORT = 8889;
	public static final int SCHEDULING_DEPT_PORT_TCP = 8890;
	public static final int MACHINE_PORT = 8891;
	public static final int MACHINE_PORT_TCP = 8892;
	public static final int MAINTENANCE_DEPT_PORT = 8893;
	public static final int MAINTENANCE_DEPT_PORT_TCP = 8894;
	public static final int ISW_PORT = 8895;
	public static final int ISW_TCP_PORT = 8896;
	public static final String SCHEDULING_DEPT_GROUP = "224.1.1.1";
	public static final String ISW_GROUP = "224.1.1.3";
	public static final String MAINTENANCE_DEPT_GROUP = "224.1.1.4";
	
	
	
	
	
	
	
	
	
	public static int TIME_SCALE_FACTOR = 1;
	public static int SHIFT_DURATION = 8;
	public static int SIMULATION_COUNT = 1000;
	
	public static void loadMacros(){
		try {
			Properties prop = new Properties();
			InputStream input = new FileInputStream("config.properties");
			prop.load(input);
			TIME_SCALE_FACTOR = Integer.parseInt(prop.getProperty("scaleFactor"));
			SHIFT_DURATION = Integer.parseInt(prop.getProperty("shiftDuration"));
			SIMULATION_COUNT = Integer.parseInt(prop.getProperty("simulationCount"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
}
