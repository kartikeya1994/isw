package org.isw.threads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.isw.FlagPacket;
import org.isw.IFPacket;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;
import org.isw.SimulationResult;

public class ListenerThread extends Thread
{
	Schedule jl;
	InetAddress schedulerIP;
	DatagramSocket udpSocket;
	ServerSocket tcpSocket;
	public ListenerThread(InetAddress schedulerIP,DatagramSocket udpSocket,ServerSocket tcpSocket) {
		this.schedulerIP = schedulerIP;
		this.udpSocket = udpSocket;
		this.tcpSocket = tcpSocket;
	}
	public void run()
	{
		DatagramPacket packet;

		try {
			udpSocket.setSoTimeout(0);	
			jl = new Schedule(InetAddress.getByName("localhost")); //FIXME: why is this localhost?
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		} catch (SocketException e) {
			e.printStackTrace();
		}

		try
		{
			while(true)
			{
				byte[] bufIn = new byte[1024];
				packet = new DatagramPacket(bufIn, bufIn.length);
				udpSocket.receive(packet); 
				byte[] reply=packet.getData();
				byte [] header = Arrays.copyOfRange(reply, 0, 4);
				final ByteArrayInputStream bais=new ByteArrayInputStream(header);
				DataInputStream dias =new DataInputStream(bais);
				int action = dias.readInt();
				if(action==Macros.SCHEDULE_PUT){
					byte[] data = Arrays.copyOfRange(reply, 4, reply.length);
					ByteArrayInputStream in = new ByteArrayInputStream(data);
					ObjectInputStream is = new ObjectInputStream(in);
					try {
						jl = (Schedule) is.readObject();	

						System.out.println("Received schedule:" + jl.printSchedule());
						System.out.println("Running Simulations");
						SimulationResult[] results = runSimulation(jl.getFarthestCompleteJob());
						System.out.println("Simulations complete");
						//wait for maintenance request
						boolean recd_req=false;
						FlagPacket fp = null;
						while(!recd_req)
						{
							fp = FlagPacket.receiveTCP(tcpSocket,0);
							if(fp == null || fp.flag!=Macros.REQUEST_IFPACKET)
								continue;
							recd_req = true;
						}
						System.out.println("Maintenance is at "+fp.ip +", sending simulation results");
						IFPacket ifPacket =  new IFPacket(results,jl,Machine.compList);
						ifPacket.send(fp.ip, fp.port);
						jl = Schedule.receive(tcpSocket); //receive PM and CM incorporated schedule from maintenance
						Thread t = new JobExecThread(jl);
						t.start();
						t.wait();
						FlagPacket.sendTCP(Macros.REQUEST_NEXT_SHIFT, schedulerIP, Macros.SCHEDULING_DEPT_PORT_TCP);
					} catch (ClassNotFoundException | InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
				}
				else if(action==Macros.SCHEDULE_GET){
					//send jobs pending from last shift to scheduler
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					ObjectOutputStream os = new ObjectOutputStream(outputStream);
					os.writeObject(jl);
					byte[] object = outputStream.toByteArray();
					DatagramPacket sendPacket = new DatagramPacket(object, object.length,schedulerIP, Macros.SCHEDULING_DEPT_PORT);
					udpSocket.send(sendPacket);
				}
			}

		}catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	private SimulationResult[] runSimulation(int maxPMO) throws InterruptedException, ExecutionException {
		SimulationResult[] results = new SimulationResult[maxPMO+1];
		for(int i=0;i<=maxPMO;i++){
			results[i]= new SimulationResult(Double.MAX_VALUE,0,0,0);
		}
		SimulationResult noPM;
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		pool.submit(new SimulationThread(jl,1,-1));
		int cnt=1;
		for(int i=0;i<=maxPMO;i++){
			for(int j = 1;j<Math.pow(2,Machine.compList.length);j++){
				pool.submit(new SimulationThread(jl,j,i));
				cnt++;
			}
		}
		for(int i=0;i<cnt;i++){
			SimulationResult result = pool.take().get();
			if(result.getPMOpportunity() < 0){
				noPM = result;
			}
			else{
				if(results[result.getPMOpportunity()].getCost() > result.getCost())
					results[result.getPMOpportunity()] = result;
			}
			}
		return results;
	}


}