package org.isw.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.isw.BruteForceAlgorithm;
import org.isw.Component;
import org.isw.Job;
import org.isw.Machine;
import org.isw.Macros;
import org.isw.Schedule;

public class ScheduleTest {

	public static void main(String[] args) throws InterruptedException, ExecutionException{
		Macros.SHIFT_DURATION = 24*60;
		Schedule schedule = new Schedule();
		Job job = new Job("J1",1,5000,Job.JOB_NORMAL);
		schedule.addJob(job);
		Job job1 = new Job("J2",1,5000,Job.JOB_NORMAL);
		schedule.addJob(job1);
		Job job2 = new Job("J3",1,5000,Job.JOB_NORMAL);
		schedule.addJob(job2);
		ArrayList<Integer> pmos = schedule.getPMOpportunities();
		
		int[] pmOpportunity = new int[pmos.size()];
		for (int i = 0; i < pmOpportunity.length; i++) {
			pmOpportunity[i] = pmos.get(i);
			System.out.println(pmOpportunity[i]);
		}
		
		
		Machine.compList = parseExcel(6);
		BruteForceAlgorithm bf = new BruteForceAlgorithm(schedule,pmOpportunity,null); 
		bf.execute();
		
		
	}
	
	private static Component[] parseExcel(int n) {
		/**
		 * Parse the component excel file into a list of components.
		 * Total number of components should be 14 for our experiment.
		 * Different component excel file for different machineNo (Stick
		 * to one for now)
		 a* 
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
}
