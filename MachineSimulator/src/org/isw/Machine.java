package org.isw;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.isw.threads.ListenerThread;

public class Machine {

	static InetAddress schedulerIP;
	static InetAddress maintenanceIP;
	
	static int machineNo;
	public static int shiftCount;
	public static Component[] compList;
	public static double cmCost;
	public static double pmCost;
	public	static long downTime;
	public static long waitTime;
	public static int jobsDone;
	public static int cmJobsDone;
	public static int pmJobsDone;
	public static int compCMJobsDone[];
	public static int compPMJobsDone[];
	public static long procCost;
	public static long penaltyCost;
	public static long cmDownTime;
	public static long pmDownTime;
	public static long runTime;
	public static long idleTime;
	
	public static void main(String[] args) {
		boolean maintenanceRegistered=false;
		boolean iswRegistered = false;
		boolean schedulerRegistered = false;
		Macros.loadMacros();
		System.out.println("Enter age of machine in hours:");
		Scanner in = new Scanner(System.in);
		int age = in.nextInt();
		System.out.println("No of components");
		int n = in.nextInt();
		try
		{
			DatagramPacket schedulerPacket  = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.REQUEST_ISW_IP);
			DatagramPacket iswPacket = FlagPacket.makePacket(Macros.ISW_GROUP, Macros.ISW_MULTICAST_PORT, Macros.MACHINE_FLAG|Macros.REQUEST_ISW_IP);
			DatagramPacket maintenancePacket = FlagPacket.makePacket(Macros.MAINTENANCE_DEPT_GROUP, Macros.MAINTENANCE_DEPT_MULTICAST_PORT, Macros.REQUEST_MAINTENANCE_DEPT_IP);
			FlagPacket packetIn;
			//create socket
			DatagramSocket socket = new DatagramSocket(Macros.MACHINE_PORT);
			ServerSocket tcpSocket = new ServerSocket(Macros.MACHINE_PORT_TCP);
			socket.setSoTimeout(3000);

			while(!schedulerRegistered || !iswRegistered || !maintenanceRegistered)
			{
				System.out.println("Finding server...");
				if(!maintenanceRegistered)
					socket.send(schedulerPacket);
				if(!iswRegistered)
					socket.send(iswPacket);
				if(!schedulerRegistered)
					socket.send(maintenancePacket);
				
				try
				{
					packetIn = FlagPacket.receiveUDP(socket);//blocking call for 1000ms
				}catch(SocketTimeoutException stoe)
				{
					System.out.println("Timed out.");
					continue; 
				}
				
				switch (packetIn.flag){
				case Macros.REPLY_MAINTENANCE_DEPT_IP:
					maintenanceIP = packetIn.ip;
					maintenanceRegistered = true;
					break;
				case Macros.REPLY_SCHEDULING_DEPT_IP:
					schedulerIP = packetIn.ip;
					schedulerRegistered = true;
					break;
				case Macros.REPLY_ISW_IP:	
					Logger.init(packetIn.ip);
					iswRegistered = true;
					break;
				}
			}
			
			//machineNo = Integer.parseInt(args[0]);
			compList = parseExcel(age,n);
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
			ListenerThread listener = new ListenerThread(schedulerIP,maintenanceIP,socket,tcpSocket);
			listener.start();


		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private static Component[] parseExcel(int age, int n) {
		/**
		 * Parse the component excel file into a list of components.
		 * Total number of components should be 14 for our experiment.
		 * Different component excel file for different machineNo (Stick
		 * to one for now)
		 * **/
		Component[] c = new Component[n];
		try
		{
			FileInputStream file = new FileInputStream(new File("Components.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			XSSFSheet labourSheet = workbook.getSheetAt(1);
			for(int i=5;i<5+n;i++)
			{
				Row row = sheet.getRow(i);
				Component comp = new Component();
				//--------CM data------------
				//0 is assembly name
				comp.compName = row.getCell(1).getStringCellValue();
				comp.initAge = row.getCell(2).getNumericCellValue();
				comp.cmEta = row.getCell(3).getNumericCellValue();
				comp.cmBeta = row.getCell(4).getNumericCellValue();
				comp.cmMuRep = row.getCell(5).getNumericCellValue();
				comp.cmSigmaRep = row.getCell(6).getNumericCellValue();
				comp.cmMuSupp = row.getCell(7).getNumericCellValue();
				comp.cmSigmaRep = row.getCell(8).getNumericCellValue();
				comp.cmRF = row.getCell(9).getNumericCellValue();
				comp.cmCostSpare = row.getCell(10).getNumericCellValue();
				comp.cmCostOther = row.getCell(11).getNumericCellValue();
				//12 is empty
				//13 is empty
				
				//--------PM data------------
				//14 is assembly name
				//15 is component name
				//16 is init age
				comp.pmMuRep = row.getCell(17).getNumericCellValue();
				comp.pmSigmaRep = row.getCell(18).getNumericCellValue();
				comp.pmMuSupp = row.getCell(19).getNumericCellValue();
				comp.pmSigmaSupp = row.getCell(20).getNumericCellValue();
				comp.pmRF = row.getCell(21).getNumericCellValue();
				comp.pmCostSpare = row.getCell(22).getNumericCellValue();
				comp.pmCostOther = row.getCell(23).getNumericCellValue();
				row = labourSheet.getRow(i);
				comp.pmLabour = new int[3];
				comp.pmLabour[0] = (int)row.getCell(3).getNumericCellValue();
				comp.pmLabour[1] = (int)row.getCell(5).getNumericCellValue();
				comp.pmLabour[2] = (int)row.getCell(7).getNumericCellValue();
				comp.cmLabour = new int[3];
				comp.cmLabour[0] = (int)row.getCell(2).getNumericCellValue();
				comp.cmLabour[1] = (int)row.getCell(4).getNumericCellValue();
				comp.cmLabour[2] = (int)row.getCell(6).getNumericCellValue();
				c[i-5] = comp;
			
			}
			file.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return c;
	}


}