package org.isw;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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
	
	public static final int SCHEDULING_DEPT_PORT = 8889;
	public static final int SCHEDULING_DEPT_PORT_TCP = 8890;
	public static final int MACHINE_PORT = 8891;
	public static final int MACHINE_PORT_TCP = 8892;
	public static final int MAINTENANCE_DEPT_PORT_TCP = 8893;
	public static final int SCHEDULING_DEPT_MULTICAST_PORT = 8888;
	public static final String MACHINE_SCHEDULING_GROUP = "224.1.1.1";
	public static final String MAINTENANCE_SCHEDULING_GROUP = "224.1.1.2";
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
