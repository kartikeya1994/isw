package org.isw.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.isw.BruteForceAlgorithm;
import org.isw.Component;
import org.isw.Job;
import org.isw.Machine;
import org.isw.MemeticAlgorithm;
import org.isw.Schedule;
import org.isw.SimulationResult;
import org.isw.threads.SimulationThread;

public class MemeticTestTime {
	static int[] pmOpportunity;
	public static void main(String[] args) throws IOException {
		try {
			Schedule schedule = new Schedule();
			Job job = new Job("J1",4,5000,Job.JOB_NORMAL);
			schedule.addJob(job);
			Job job1 = new Job("J2",3,5000,Job.JOB_NORMAL);
			schedule.addJob(job1);
			Job job2 = new Job("J3",1,5000,Job.JOB_NORMAL);
			schedule.addJob(job2);
			int arr[] ={7};
			//FileWriter writer = new FileWriter("mavsbf.csv");
			for(int c=0; c<arr.length; c++){
				System.out.println("\n***************************");
				System.out.format("No of components: %d\n", arr[c]);
				System.out.println("***************************");
				Machine.compList = parseExcel(arr[c]);
				ExecutorService threadPool = Executors.newSingleThreadExecutor();
				CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
				pool.submit(new SimulationThread(schedule,null,null,true,-1));
				SimulationResult result;
				result = pool.take().get();
				threadPool.shutdown();
				while(!threadPool.isTerminated());
				System.out.println("Cost of no PM: "+result.cost);

				ArrayList<Integer> pmos = schedule.getPMOpportunities();
				pmOpportunity = new int[pmos.size()];
				for (int i = 0; i < pmOpportunity.length; i++) {
					pmOpportunity[i] = pmos.get(i);
				}


				if(pmOpportunity.length > 0){

					/*	for(int n =100;n<1000;n+=50){
							Double mean = 0d;
							double[] costs = new double[n];
						for(int i=0;i<n;i++){
							costs[i] = simulateSolution(schedule,6);
							mean += costs[i];
						}
						mean/=n;
						double sd = 0d;
						for(int i=0;i<n;i++){
							sd += Math.pow(costs[i]-mean,2);
						}
						sd/=(n-1);
						sd = Math.pow(sd, 0.5);
						writer.append(String.format("%d,%f,%f\n",n,mean,sd));
						System.out.println("Mean: "+mean);
						System.out.println("SD: "+sd);
					}
					writer.flush();
				    writer.close();
					 */
					Long time = System.nanoTime();
//					MemeticAlgorithm ma = new MemeticAlgorithm(pmOpportunity.length*Machine.compList.length*2,200,schedule,pmOpportunity,result);
//					ma.execute();
//					double MATime = (System.nanoTime() - time)/Math.pow(10, 9);
//					System.out.format("MA Time: %f\n", MATime);
//					// write results to file
//					try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("MATime.csv", true)))) {
//						out.format("%d,%f\n",arr[c],MATime);
//					}catch (IOException e) {
//						//exception handling left as an exercise for the reader
//					}
					
					time = System.nanoTime();
					BruteForceAlgorithm bf = new BruteForceAlgorithm(schedule,pmOpportunity,result); 
					System.out.println("No. of PM opportunities: "+pmOpportunity.length);
					bf.execute();
					double BFTime = (System.nanoTime() - time)/Math.pow(10, 9);
					System.out.format("BF Time: %f\n",BFTime);
					// write results to file
					try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("BFTime.csv", true)))) {
						out.format("%d,%f\n",arr[c],BFTime);
					}catch (IOException e) {
						//exception handling left as an exercise for the reader
					}
					
				}
			}

		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static Component[] parseExcel(int n) {
		/**
		 * Parse the component excel file into a list of components.
		 * Total number of components should be 14 for our experiment.
		 * Different component excel file for different machineNo (Stick
		 * to one for now)
		 * 
		 * */
		Component[] c = new Component[n];
		try
		{
			FileInputStream file = new FileInputStream(new File("components.xlsx"));
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
				comp.cmSigmaSupp = row.getCell(8).getNumericCellValue();
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
				comp.labourCost = new double[]{800,500,300};
				comp.pmLabour = new int[3];
				comp.pmLabour[0] = (int)row.getCell(3).getNumericCellValue();
				comp.pmLabour[1] = (int)row.getCell(5).getNumericCellValue();
				comp.pmLabour[2] = (int)row.getCell(7).getNumericCellValue();
				comp.cmLabour = new int[3];
				comp.cmLabour[0] = (int)row.getCell(2).getNumericCellValue();
				comp.cmLabour[1] = (int)row.getCell(4).getNumericCellValue();
				comp.cmLabour[2] = (int)row.getCell(6).getNumericCellValue();
				comp.initProps(i-5);
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
	static double simulateSolution(Schedule jl,long combo) throws InterruptedException, ExecutionException{
		ExecutorService threadPool = Executors.newSingleThreadExecutor();
		CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
		pool.submit(new SimulationThread(jl,getComboList(combo),pmOpportunity,false,combo));
		SimulationResult result = pool.take().get();
		//System.out.format("Cost: %f (%s)\n", result.cost, Long.toBinaryString(combo));
		threadPool.shutdown();
		while(!threadPool.isTerminated());
		return result.cost;
	}

	private  static long[] getComboList(long combo) {
		long combos[] = new long[pmOpportunity.length];
		for(int i =0;i<pmOpportunity.length;i++){
			combos[i] = (combo>>(Machine.compList.length*i))&((int)Math.pow(2,Machine.compList.length)-1);
		}
		return combos;
	}
}
