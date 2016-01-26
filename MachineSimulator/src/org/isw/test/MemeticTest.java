package org.isw.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import org.isw.Macros;
import org.isw.MemeticAlgorithm;
import org.isw.Schedule;
import org.isw.SimulationResult;
import org.isw.threads.SimulationThread;

public class MemeticTest {
	static int[] pmOpportunity;
	public static void main(String[] args) throws IOException {
		try {
			Macros.SHIFT_DURATION = 24*60;
			Schedule schedule = new Schedule();
			for(int i=0;i<10;i++){
				Job j = new Job("J"+String.valueOf(i+1),144,5000,Job.JOB_NORMAL);
				j.setPenaltyCost(200);
				schedule.addJob(j);
			}
			
			int arr[] ={4};
			//FileWriter writer = new FileWriter("mavsbf.csv");
			for(int c=0;c<arr.length;c++){
				Machine.compList = parseExcel(arr[c]);
				ExecutorService threadPool = Executors.newSingleThreadExecutor();
				CompletionService<SimulationResult> pool = new ExecutorCompletionService<SimulationResult>(threadPool);
				pool.submit(new SimulationThread(schedule,null,null,true,-1));
				SimulationResult result;
				result = pool.take().get();
				threadPool.shutdown();
				while(!threadPool.isTerminated());
				System.out.println("Cost of no PM: " + result.cost);
				ArrayList<Integer> pmos = schedule.getPMOpportunities();
				pmOpportunity = new int[pmos.size()];
				for (int i = 0; i < pmOpportunity.length; i++) {
					pmOpportunity[i] = pmos.get(i);
				}
				System.out.format("m: %d n: %d\n",pmOpportunity.length,Machine.compList.length);
					if(pmOpportunity.length > 0){
					Long time = System.nanoTime();
					System.out.println("MA:");
					MemeticAlgorithm ma = new MemeticAlgorithm(pmOpportunity.length*Machine.compList.length*2,200,schedule,pmOpportunity,result,false);
					ma.execute();
					System.out.format("time: %f\n",(System.nanoTime() - time)/Math.pow(10, 9));
					
					//time = System.nanoTime();
					//System.out.println("GA:");
					//ma = new MemeticAlgorithm(pmOpportunity.length*Machine.compList.length*2,200,schedule,pmOpportunity,result,true);
					//ma.execute();
					//System.out.format("time: %f\n",(System.nanoTime() - time)/Math.pow(10, 9));
					
					System.out.println("BF:");
					time = System.nanoTime();
					BruteForceAlgorithm bf = new BruteForceAlgorithm(schedule,pmOpportunity,result); 
					bf.execute();
					System.out.format("time: %f\n",(System.nanoTime() - time)/Math.pow(10, 9));	
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
