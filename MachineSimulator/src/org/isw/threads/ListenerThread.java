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
import java.util.ArrayList;
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
	
	private InetAddress maintenanceIP =null;
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
				byte[] bufIn = new byte[4096];
				packet = new DatagramPacket(bufIn, bufIn.length);
				udpSocket.receive(packet); 
				byte[] reply=packet.getData();
				byte [] header = Arrays.copyOfRange(reply, 0, 4);
				final ByteArrayInputStream bais=new ByteArrayInputStream(header);
				DataInputStream dias =new DataInputStream(bais);
				int action = dias.readInt();
				switch(action){
				case Macros.PROCESS_COMPLETE:
					writeResults();
					break;
				case Macros.MAINTENANCE_DEPT_IP:
					maintenanceIP  = packet.getAddress();
					break;
				case Macros.REPLY_NEXT_SHIFT:	
					byte[] data = Arrays.copyOfRange(reply, 4, reply.length);
					ByteArrayInputStream in = new ByteArrayInputStream(data);
					ObjectInputStream is = new ObjectInputStream(in);
					try {
						jl = (Schedule) is.readObject();	
						System.out.println("Received schedule from scheduler:" + jl.printSchedule());
						System.out.println("Total time" + jl.getSum());
						System.out.println("Running Simulations");
						long starttime = System.currentTimeMillis();
						SimulationResult[] results = null;
						results = runSimulation(jl.getPMOpportunities());
						System.out.println("Simulations complete in " +(System.currentTimeMillis() - starttime));
						System.out.println("Sending simulation results to Maintenance");
						IFPacket ifPacket =  new IFPacket(results,jl,Machine.compList);
						ifPacket.send(maintenanceIP, Macros.MAINTENANCE_DEPT_PORT_TCP);
						jl = Schedule.receive(tcpSocket); //receive PM and CM incorporated schedule from maintenance
						System.out.println("Received schedule from maintenance:" + jl.printSchedule());
						System.out.println("Total time" + jl.getSum());
						ExecutorService threadPool = Executors.newSingleThreadExecutor();
						threadPool.execute(new JobExecThread(jl));
						threadPool.shutdown();
						while(!threadPool.isTerminated()); 
						Machine.shiftCount++;
						FlagPacket.sendTCP(Macros.REQUEST_NEXT_SHIFT, schedulerIP, Macros.SCHEDULING_DEPT_PORT_TCP);
					} catch (ClassNotFoundException | InterruptedException | ExecutionException e) {
						e.printStackTrace();
					}
					break;
				case Macros.REQUEST_PREVIOUS_SHIFT:
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
	private void writeResults() {
		System.out.println("=========================================");
		System.out.println("Downtime:" + String.valueOf(Machine.downTime*100/(Machine.runTime)) +"%");
		System.out.println("CM Downtime: "+ Machine.cmDownTime +" hours");
		System.out.println("PM Downtime: "+ Machine.pmDownTime +" hours");
		System.out.println("Waiting Downtime: "+ Machine.waitTime +" hours");
		System.out.println("Number of jobs:" + Machine.jobsDone);
		System.out.println("Number of CM jobs:" + Machine.cmJobsDone);
		System.out.println("Number of PM jobs:" + Machine.pmJobsDone);
	}
	private SimulationResult[] runSimulation(ArrayList<Integer> pmoList) throws InterruptedException, ExecutionException {
		if(pmoList.isEmpty()){
		SimulationResult[] results ={new SimulationResult(Double.MAX_VALUE,0,1,-1)};
		return results;
		}
		
		SimulationResult[] results = new SimulationResult[pmoList.size()];
		for(int i=0;i<pmoList.size();i++){
			results[i]= new SimulationResult(Double.MAX_VALUE,0,1,-1);
		}
		
		SimulationResult noPM = null;
		ExecutorService threadPool = Executors.newFixedThreadPool(20);
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		pool.submit(new SimulationThread(jl,1,-1));
		int cnt=1;
		for(Integer i : pmoList){
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
				if(results[pmoList.indexOf(result.getPMOpportunity())].getCost() > result.getCost())
					results[pmoList.indexOf(result.getPMOpportunity())] = result;
			}
			}
		//Calculate IFs
		for(int i=0;i<results.length;i++){
			results[i].cost = noPM.cost*(results[i].cost - noPM.cost);
		}
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		return results;
	}


}