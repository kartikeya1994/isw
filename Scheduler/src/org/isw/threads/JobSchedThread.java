package org.isw.threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.PriorityQueue;
import java.util.Random;

import org.isw.Job;
import org.isw.MachineList;
import org.isw.Schedule;

public class JobSchedThread extends Thread
{
	final static int SCHED_PUT = 3;
	final static int SCHED_GET = 4;
	MachineList machineList;
	Random r = new Random();
	DatagramSocket socket;
	ArrayList<Job> jobArray; 
	public JobSchedThread(MachineList machineList)
	{
		this.machineList=machineList;
	}
	
	public void run()
	{	
		
		try {
			socket = new DatagramSocket(8889);
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		while(true)
		{	
			
			getJobs();
			PriorityQueue<Schedule> pq = new PriorityQueue<Schedule>();
		
			Enumeration<InetAddress> en = machineList.getIPs();
			while(en.hasMoreElements())
			{	
				InetAddress ip = en.nextElement();
				try {
					
				final ByteArrayOutputStream baos=new ByteArrayOutputStream();
				final DataOutputStream daos=new DataOutputStream(baos);
				daos.writeInt(SCHED_GET);
				daos.close();
				final byte[] bufOut=baos.toByteArray();
				DatagramPacket packetOut = new DatagramPacket(bufOut, bufOut.length, ip, 8889);
				socket.send(packetOut);
				
				byte[] bufIn = new byte[1024];
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
				System.out.print(jobArray.get(i).getJobName()+": "+jobArray.get(i).getJobTime()/60+" ");
				
				pq.add(min);
			}
			System.out.println("");
			while(!pq.isEmpty()){
	            try {
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		            ObjectOutputStream os = new ObjectOutputStream(outputStream);
		            os.writeObject(pq.peek());
		            byte[] object = outputStream.toByteArray();
		            os.close();
		            outputStream.reset();
		            DataOutputStream ds = new DataOutputStream(outputStream);
		            ds.writeInt(SCHED_PUT);
		            byte[] header =outputStream.toByteArray();
		            ds.close();
		            outputStream.reset();
		            outputStream.write( header );
		            outputStream.write( object );
		            byte[] data = outputStream.toByteArray( );
		            DatagramPacket sendPacket = new DatagramPacket(data, data.length, pq.poll().getAddress(), 8889);
		            socket.send(sendPacket);
	            } catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			
			try
			{
				sleep(8*3600+40); // sleep for 8 hours plus extra time for network lag
			}catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}
	void getJobs(){
		jobArray = new ArrayList<Job>();
		for(int i=14;i>0;i--){ 
			if(r.nextBoolean()){
				Job job = new Job(String.valueOf(i),i*1200,i*1000,Job.JOB_NORMAL);
				job.setPenaltyCost(i*500);
				jobArray.add(job);
				}
		}
	}
}
