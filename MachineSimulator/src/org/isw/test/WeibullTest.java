package org.isw.test;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Random;

public class WeibullTest {
	public static void main(String[] args) {
		//crossover();
		Random rnd = new Random();
		Hashtable<BigInteger, Boolean> hashTable = new Hashtable<BigInteger, Boolean>();
		BigInteger a = new BigInteger(6, rnd);
		hashTable.put(a, true);
		System.out.println(a);
		BigInteger b = new BigInteger(6, rnd);
		hashTable.put(b, true);
		System.out.println(b);
		a = new BigInteger(6, rnd);
		System.out.println(a);
		System.out.println(hashTable.containsKey(b));
		
	}
	
	//Single point crossover
		public static void crossover() {
			BigInteger rights[] = {new BigInteger("78"),new BigInteger("84")};
			BigInteger lefts[] = {new BigInteger("78"),new BigInteger("84")};

			int crossoverPoint = 6;
			
			rights[0] = rights[0].and(BigInteger.valueOf((1<<crossoverPoint)-1));
			rights[1] = rights[1].and(BigInteger.valueOf((1<<crossoverPoint)-1));
			
			lefts[0] = lefts[0].shiftRight(crossoverPoint).shiftLeft(crossoverPoint);
			lefts[1] = lefts[1].shiftRight(crossoverPoint).shiftLeft(crossoverPoint);
			
//			for(int i=0; i<Machine.compList.length*pmOpportunity.length;i++){
//				if(i < crossoverPoint){
//					combo2[0] = combo2[0] & ~(1<<i);
//					combo1[1] = combo2[1] & ~(1<<i);
//				}
//				else{
//					combo1[0] = combo1[0] & ~(1<<i);
//					combo2[1] = combo2[1] & ~(1<<i);
//				}
//			}	

			System.out.println(lefts[0].or(rights[1]));
			System.out.println(lefts[1].or(rights[0]));
			
			
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
