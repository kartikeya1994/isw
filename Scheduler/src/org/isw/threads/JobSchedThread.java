package org.isw.threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.PriorityQueue;
import java.util.Random;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.isw.FlagPacket;
import org.isw.Job;
import org.isw.MachineList;
import org.isw.Macros;
import org.isw.Schedule;

public class JobSchedThread extends Thread
{
	final static int SCHED_PUT = 3;
	final static int SCHED_GET = 4;
	MachineList machineList;
	Random r = new Random();
	DatagramSocket socket;
	ServerSocket tcpSocket;
	long procTimeArr[]={5,5,5,4,4,4,4,4,2,2,1,1};
	int procCostArr[]={80,80,70,70,70,60,60,50,50,40,40,40};
	ArrayList<Job> jobArray;
	int shiftCount;
	public JobSchedThread(MachineList machineList, int shiftCount)
	{
		this.machineList=machineList;
		this.shiftCount = shiftCount;
	}

	public void run()
	{	
		

		try {
			socket = new DatagramSocket(Macros.SCHEDULING_DEPT_PORT);
			tcpSocket = new ServerSocket(Macros.SCHEDULING_DEPT_PORT_TCP);
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int cnt = 0;
		parseJobs();
		while(cnt++ < shiftCount)
		{	
			PriorityQueue<Schedule> pq = new PriorityQueue<Schedule>();
			Enumeration<InetAddress> en = machineList.getIPs();
			while(en.hasMoreElements())
			{	
				InetAddress ip = en.nextElement();
				try {
					//request pending jobs from previous shift from machine
					final ByteArrayOutputStream baos=new ByteArrayOutputStream();
					final DataOutputStream daos=new DataOutputStream(baos);
					daos.writeInt(Macros.REQUEST_PREVIOUS_SHIFT);
					daos.close();
					final byte[] bufOut=baos.toByteArray();
					DatagramPacket packetOut = new DatagramPacket(bufOut, bufOut.length, ip, Macros.MACHINE_PORT);
					socket.send(packetOut);
					System.out.println("Sent schedule get req to machines");
					byte[] bufIn = new byte[4096*8];
					DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
					socket.receive(packet);
					byte[] object = packet.getData();
					ByteArrayInputStream in = new ByteArrayInputStream(object);
					ObjectInputStream is = new ObjectInputStream(in);
					Schedule jl = (Schedule) is.readObject();	
					jl.setAddress(ip);
					pq.add(jl);	
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} 

			}

			System.out.println("Job list: ");
			for(int i=0;i<jobArray.size();i++){
				Schedule min = pq.remove();
				min.addJob(jobArray.get(i));
				System.out.print(jobArray.get(i).getJobName()+": "+String.valueOf(jobArray.get(i).getJobTime()/Macros.TIME_SCALE_FACTOR)+" ");
				pq.add(min);
			}

			while(!pq.isEmpty()){
				try {
					//send job list to machine
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					ObjectOutputStream os = new ObjectOutputStream(outputStream);
					os.writeObject(pq.peek());
					byte[] object = outputStream.toByteArray();
					os.close();
					outputStream.reset();
					DataOutputStream ds = new DataOutputStream(outputStream);
					ds.writeInt(Macros.REPLY_NEXT_SHIFT);
					byte[] header =outputStream.toByteArray();
					ds.close();
					outputStream.reset();
					outputStream.write( header );
					outputStream.write( object );
					byte[] data = outputStream.toByteArray( );
					DatagramPacket sendPacket = new DatagramPacket(data, data.length, pq.peek().getAddress(), Macros.MACHINE_PORT);
					System.out.println("Sending schedule to "+pq.poll().getAddress());
					socket.send(sendPacket);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

			FlagPacket fp;
			int count =0;
			while(count < machineList.count()){
				fp = FlagPacket.receiveTCP(tcpSocket,0);
				if(fp.flag == Macros.REQUEST_NEXT_SHIFT)
					count++;
			}
		}
		System.out.println("Process Complete");
		Enumeration<InetAddress> en = machineList.getIPs();
		while(en.hasMoreElements()){
			DatagramPacket dp = FlagPacket.makePacket(en.nextElement().getHostAddress(), Macros.MACHINE_PORT, Macros.PROCESS_COMPLETE);
			try {
				socket.send(dp);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	private void parseJobs() {
		jobArray = new ArrayList<Job>();
		try
		{
			FileInputStream file = new FileInputStream(new File("Jobs.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);
			
			for(int i=1;i<=12;i++)
			{
				Row row = sheet.getRow(i);
				int demand = (int) row.getCell(5).getNumericCellValue();
				String jobName = row.getCell(0).getStringCellValue();
				long jobTime = (long)(row.getCell(1).getNumericCellValue()*Macros.TIME_SCALE_FACTOR);
				double jobCost = row.getCell(3).getNumericCellValue();
				for(int j=0; j<demand ;j++){
					Job job = new Job(jobName,jobTime,jobCost,Job.JOB_NORMAL);
					job.setPenaltyCost(row.getCell(4).getNumericCellValue());
					jobArray.add(job);
				}
			}
			file.close();
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}		
		
	}

	
}
