package org.isw.threads;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import org.isw.Component;
import org.isw.FlagPacket;
import org.isw.Job;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;

public class JobExecThread extends Thread{
	Schedule jobList;
	DatagramSocket socket;
	DatagramPacket timePacket;
	public JobExecThread(Schedule jobList , DatagramSocket socket){
		this.jobList = jobList;
		this.socket = socket;
		timePacket = FlagPacket.makePacket(Macros.SCHEDULING_DEPT_GROUP, Macros.SCHEDULING_DEPT_MULTICAST_PORT, Macros.REQUEST_TIME);
	}
	
	public void run(){
		int time=0;

		while(!jobList.isEmpty() && time < Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR){

			Job current = jobList.peek(); 
			try{
				jobList.decrement(1);
			}
			catch(IOException e){
				e.printStackTrace();
				System.exit(0);
			}
			
			switch(current.getJobType())
			{
			/*
			 * Increment costs according to models depending upon job type
			 */
			case Job.JOB_NORMAL:
				Machine.procCost += current.getJobCost()/Macros.TIME_SCALE_FACTOR;
				for(Component comp : Machine.compList)
					comp.initAge++;
				break;
			case Job.JOB_PM:
				Machine.pmCost += current.getFixedCost() + current.getJobCost()*current.getJobTime()/Macros.TIME_SCALE_FACTOR;
				Machine.pmDownTime++;
				Machine.downTime++;
				break;
			case Job.JOB_CM:
				Machine.cmCost += current.getFixedCost() + current.getJobCost()*current.getJobTime()/Macros.TIME_SCALE_FACTOR;
				current.setFixedCost(0);
				Machine.downTime++;
				Machine.cmDownTime++;
				break;
			case Job.WAIT_FOR_MT:
				Machine.downTime++;
				Machine.waitTime++;
			}
			
			if(current.getJobTime()<=0)
			{
				//Job ends here
				switch(current.getJobType())
				{
				case Job.JOB_PM:
					// decrease the age for each component that underwent PM
					for(int i =0; i<Machine.compList.length;i++){
						int pos=1<<i;
						int bitmask = current.getCompCombo();
						if((pos&bitmask) != 0){
							Component comp = Machine.compList[i];
							comp.initAge = (1-comp.pmRF)*comp.initAge;
							Machine.compPMJobsDone[i]++;
						}
					}
					Machine.pmJobsDone++;
					break;
					
				case Job.JOB_CM:
					// 
					Component comp = Machine.compList[current.getCompNo()];
					comp.initAge = (1 - comp.cmRF)*comp.initAge;
					Machine.cmJobsDone++;
					Machine.compCMJobsDone[current.getCompNo()]++;
					break;
				case Job.JOB_NORMAL:
					Machine.jobsDone++;
					break;
				}
				try{
					System.out.println("Job "+ jobList.remove().getJobName()+" complete");
				}
				catch(IOException e){
					e.printStackTrace();
					System.exit(0);
				}
			}

			time++;
			Machine.runTime++;
			try {
				byte[] bufIn = new byte[128];
				DatagramPacket packet = new DatagramPacket(bufIn, bufIn.length);
				socket.send(timePacket);
				socket.receive(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		if(jobList.isEmpty()){
			Machine.idleTime += Macros.SHIFT_DURATION*Macros.TIME_SCALE_FACTOR - time;
			return;
		}
		int i = jobList.indexOf(jobList.peek());
		while(i < jobList.getSize()){
			Machine.penaltyCost += jobList.jobAt(i++).getPenaltyCost()*Macros.SHIFT_DURATION;
		}

	}


}