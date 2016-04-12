package org.isw.test;

import org.isw.LabourAvailability;

public class AvailTest {
	
	public static void main(String args[])
	{
		int labour[] = {5,2,3};
		int labour2[] = {5,1,3};
		int labour3[] = {5,2,4};
		int labour4[] = {0,1,0};
		LabourAvailability la = new LabourAvailability(labour, 20);
		System.out.println(la.checkAvailability(3, 8, labour2));
		la.employLabour(3, 8, labour2);
		la.print();
		System.out.println(la.checkAvailability(3, 4, labour3));
		la.employLabour(6, 8, labour4);
		la.print();
	}
}
