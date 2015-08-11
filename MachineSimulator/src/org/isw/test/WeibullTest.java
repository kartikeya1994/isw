package org.isw.test;

import java.util.Random;

public class WeibullTest {
	public static void main(String[] args) {
		for(int i=0;i<4;i++)
		System.out.println(weibull(2.3,4000,900000));
	}
	public static double weibull(double p, double q, double agein) 
	{		
		//p beta and q eta 
		double t0 = agein;
		double b=Math.pow(t0, p);
		double a=Math.pow((1/q), p);
		Random x= new Random();
		return (Math.pow(b-((Math.log(1-x.nextDouble())/a)),(1/p)))-t0;
	}
}
