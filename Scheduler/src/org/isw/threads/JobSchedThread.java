package org.isw.threads;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
import org.isw.MaintenanceRequestPacket;
import org.isw.MaintenanceTuple;
import org.isw.Schedule;
import org.isw.SchedulingDept;

public class JobSchedThread extends Thread
{
	MachineList machineList;
	Random r = new Random();
	ServerSocket tcpSocket;
	ArrayList<Job> jobArray;
	int shiftCount;
	public JobSchedThread(MachineList machineList, int shiftCount)
	{
		this.machineList= machineList;
		this.shiftCount = shiftCount;
	}

	public void run()
	{	
		try 
		{
			tcpSocket = new ServerSocket(Macros.SCHEDULING_DEPT_PORT_TCP);
		} catch (SocketException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		parseJobs(); // from Excel sheet
		
		int shiftsDone  = 0;
		while(shiftsDone < shiftCount)
		{	
			doScheduling();
			// schedules are sent
			
			FlagPacket fp;
			int count = 0;
			boolean replan = false;
			while(count < machineList.count())
			{
				fp = FlagPacket.receiveTCP(tcpSocket,0);
				if(fp.flag == Macros.SCHED_REPLAN_INIT)
					replan = true;
				else if(fp.flag != Macros.REQUEST_NEXT_SHIFT)
					continue;
				
				count++;
			}
			if(replan)
			{
				// -3 packet to Maintenance indicating replan
				MaintenanceRequestPacket mrp = new MaintenanceRequestPacket(SchedulingDept.maintenanceIP, 
						Macros.MAINTENANCE_DEPT_PORT_TCP, new MaintenanceTuple(-3));
				mrp.sendTCP();
				continue;
			}
			
			shiftsDone++;
			
			//Let maintenance know that shift is over
			MaintenanceRequestPacket mrp = new MaintenanceRequestPacket(SchedulingDept.maintenanceIP, 
					Macros.MAINTENANCE_DEPT_PORT_TCP, new MaintenanceTuple(-1));
			mrp.sendTCP();
		}

		System.out.println("Process Complete");
		SchedulingDept.processComplete = true;
		Enumeration<InetAddress> en = machineList.getIPs();
		while(en.hasMoreElements())
		{
			FlagPacket.sendTCP(Macros.PROCESS_COMPLETE, en.nextElement(), Macros.MACHINE_PORT_TCP);
		}
	}

	private void doScheduling()
	{
		// Priority Queue fetches schedules with least total job time
		PriorityQueue<Schedule> pq = new PriorityQueue<Schedule>();
		Enumeration<InetAddress> machineIPs = machineList.getIPs();
		while(machineIPs.hasMoreElements())
		{	
			//collect remaining schedules from machines
			InetAddress ip = machineIPs.nextElement();
			FlagPacket.sendTCP(Macros.REQUEST_PREVIOUS_SHIFT, ip, Macros.MACHINE_PORT_TCP);		
			System.out.println("Getting leftover jobs from machine: "+ip);
			try 
			{
				Schedule jl = Schedule.receive(tcpSocket);

				collectRemainingJobs(jl);

				jl.setAddress(ip);

				pq.add(jl);

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		//sort jobs in descending order of job time
		Collections.sort(jobArray, new JobComparator());

		System.out.println("Jobs to rearrange: ");
		for(int i=0;i<jobArray.size();i++){
			/*
			 * Generate new schedule: Get schedule with minimum total time,
			 * and add the first job in jobArray to it.
			 */
			Schedule min = pq.remove(); 
			min.addJob(jobArray.get(i));
			System.out.print(jobArray.get(i).getJobName()+": "+String.valueOf(jobArray.get(i).getJobTime()/Macros.TIME_SCALE_FACTOR)+" ");
			pq.add(min);
		}
		jobArray = new ArrayList<Job>(); //empty jobArray
		System.out.println();

		//send schedules back to machines
		while(!pq.isEmpty())
		{
			try {
				pq.peek().send(pq.peek().getAddress(), Macros.MACHINE_PORT_TCP);
				System.out.println("Sending schedule to "+pq.poll().getAddress());
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void collectRemainingJobs(Schedule jl) throws IOException 
	{
		if(jl.isEmpty())
			return;
		Job first = jl.peek();
		int start = 0;
		if( first.getJobType() == Job.JOB_CM ||
				(first.getJobType() == Job.JOB_NORMAL && first.getStatus() == Job.STARTED))
		{
			// Dont remove first job if CM job or Normal Job that was started
			start = 1;
			if(first.getJobType() == Job.JOB_CM 
					&& jl.jobAt(1).getJobType() == Job.JOB_NORMAL 
					&& jl.jobAt(1).getStatus() == Job.STARTED)
				start = 2; //CM job followed by started normal job
				
		}

		else if(first.getJobType() == Job.JOB_PM && 
				(first.getStatus() == Job.STARTED || first.getStatus() == Job.SERIES_STARTED))
		{
			// if PM job was started, remove entire series
			while(first.getJobType() == Job.JOB_PM)
				first = jl.jobAt(++start);	
		}

		//add jobs that were not started to jobArray
		for(int r=jl.getSize()-1; r>=start; r--)
			jobArray.add(jl.remove(r));

	}


	private void parseJobs() 
	{
		/*
		 * Get jobs from Excel sheet
		 */
		jobArray = new ArrayList<Job>();
		try
		{
			FileInputStream file ;
			if(Macros.NO_OF_JOBS == 7)
				file = new FileInputStream(new File("Jobs.xlsx"));
			else 
				file = new FileInputStream(new File("Jobs_3.xlsx"));
			XSSFWorkbook workbook = new XSSFWorkbook(file);
			XSSFSheet sheet = workbook.getSheetAt(0);

			for(int i=1;i<=machineList.count()*Macros.NO_OF_JOBS;i++)
			{
				Row row = sheet.getRow(i);
				String jobName = row.getCell(0).getStringCellValue();
				long jobTime = (long)(row.getCell(1).getNumericCellValue()*Macros.TIME_SCALE_FACTOR);
				double jobCost = row.getCell(3).getNumericCellValue();	
				Job job = new Job(jobName,jobTime,jobCost,Job.JOB_NORMAL);
				job.setPenaltyCost(row.getCell(4).getNumericCellValue());
				jobArray.add(job);
			}
			file.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}	
	}
}

class JobComparator implements Comparator<Job> {
	/*
	 * Sort jobs in descending order of job time
	 */
	@Override
	public int compare(Job a, Job b) 
	{
		return Double.compare(a.getJobTime(),b.getJobTime());
	}


}
