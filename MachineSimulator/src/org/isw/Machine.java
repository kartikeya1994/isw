package org.isw;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

	static InetAddress serverIP;
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
		boolean registered=false;
		Macros.loadMacros();
		System.out.println("Enter age of machine in hours:");
		Scanner in = new Scanner(System.in);
		int age = in.nextInt();
		System.out.println("No of components");
		int n = in.nextInt();
		try
		{
			
			//create outbound packet with HELLO message
			final ByteArrayOutputStream baos=new ByteArrayOutputStream();
			final DataOutputStream dos=new DataOutputStream(baos);
			dos.writeInt(Macros.REQUEST_SCHEDULING_DEPT_IP);
			dos.close();
			final byte[] buf=baos.toByteArray();
			InetAddress group = InetAddress.getByName(Macros.MACHINE_SCHEDULING_GROUP);
			DatagramPacket packetOut, packetIn;
			packetOut = new DatagramPacket(buf, buf.length, group, Macros.SCHEDULING_DEPT_MULTICAST_PORT);

			//create socket
			DatagramSocket socket = new DatagramSocket(Macros.MACHINE_PORT);
			ServerSocket tcpSocket = new ServerSocket(Macros.MACHINE_PORT_TCP);
			socket.setSoTimeout(3000);

			while(!registered)
			{
				System.out.println("Finding server...");
				socket.send(packetOut);

				byte[] resbuf = new byte[256];
				packetIn = new DatagramPacket(resbuf, resbuf.length);
				try
				{
					socket.receive(packetIn); //blocking call for 1000ms
				}catch(SocketTimeoutException stoe)
				{
					System.out.println("Timed out.");
					continue;
				}
				final byte[] reply=packetIn.getData();
				final ByteArrayInputStream bais=new ByteArrayInputStream(reply);
				final DataInputStream dis=new DataInputStream(bais);

				if(dis.readInt() == Macros.REPLY_SCHEDULING_DEPT_IP)
				{
					registered=true;
					serverIP=packetIn.getAddress();
					System.out.println("Connected to server: "+serverIP.toString());

					//socket.close();
				}
				else
					continue;
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
			ListenerThread listener = new ListenerThread(serverIP,socket,tcpSocket);
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
			
			for(int i=4;i<4+n;i++)
			{
				Row row = sheet.getRow(i);
				Component comp = new Component();
				comp.compName = row.getCell(1).getStringCellValue();
				comp.compCost = row.getCell(15).getNumericCellValue();
				
				comp.p1 = row.getCell(2).getNumericCellValue();
				comp.p2 = row.getCell(3).getNumericCellValue();
				comp.p3 = row.getCell(4).getNumericCellValue();
				
				comp.cmEta = row.getCell(5).getNumericCellValue();
				comp.cmBeta = row.getCell(6).getNumericCellValue();
				
				comp.cmMu = row.getCell(7).getNumericCellValue();
				comp.cmSigma = row.getCell(8).getNumericCellValue();
				comp.cmRF = row.getCell(9).getNumericCellValue();
				comp.cmCost = row.getCell(10).getNumericCellValue();
				
				comp.pmMu = row.getCell(11).getNumericCellValue();
				comp.pmSigma = row.getCell(12).getNumericCellValue();
				comp.pmRF = row.getCell(13).getNumericCellValue();
				comp.pmCost = row.getCell(14).getNumericCellValue();
				
				comp.pmFixedCost = row.getCell(16).getNumericCellValue();
				comp.cmFixedCost = row.getCell(17).getNumericCellValue();
				comp.initAge = age;
				c[i-4] = comp;
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