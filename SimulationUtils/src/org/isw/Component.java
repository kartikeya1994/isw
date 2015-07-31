package org.isw;

import java.io.Serializable;
import java.util.Random;

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;


public class Component implements Serializable {
	/**
	 * 
	 */
	public static final long serialVersionUID = 1L;
	public String compName;

	//CM
	// TOF
	public double cmEta;
	public double cmBeta;
	// TTR
	public double cmMuRep;
	public double cmSigmaRep;
	public double cmMuSupp;
	public double cmSigmaSupp;
	public double cmRF;
	public double cmCostSpare;
	public double cmCostOther;
	//PM
	// TTR
	public double pmMuRep;
	public double pmSigmaRep;
	public double pmMuSupp;
	public double pmSigmaSupp;
	public double pmRF;
	public double pmCostSpare;
	public double pmCostOther;

	public int[] pmLabour;
	public int[] cmLabour;
	public double[] labourCost;
	public double initAge;

	public double getPMTTR(){
		return normalRandom(pmMuRep,pmSigmaRep) + normalRandom(pmMuSupp,pmSigmaSupp);
	}
	
	public double getCMTTR(){
		return normalRandom(cmMuRep,cmSigmaRep) + normalRandom(cmMuSupp,cmSigmaSupp);
	}
	
	public double getCMTTF(){
		return weibull(cmBeta,cmEta,initAge);
	}
	
	public static double normalRandom(double mean, double sd)				
	{
	RandomGenerator rg = new JDKRandomGenerator();
	GaussianRandomGenerator g= new GaussianRandomGenerator(rg);	
	double a=(double) (mean+g.nextNormalizedDouble()*sd);
	return a;
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

	public double getPMFixedCost() {
		return pmCostSpare+pmCostOther;
	}

	public double getCMFixedCost() {
		return cmCostSpare+cmCostOther;
	}

	public double getCMLabourCost() {
		return cmLabour[0]*labourCost[0] + cmLabour[1]*labourCost[1] + cmLabour[2]*labourCost[2];
	}

	public double getPMLabourCost() {
		return pmLabour[0]*labourCost[0] + pmLabour[1]*labourCost[1] + pmLabour[2]*labourCost[2];
	}

	public int[] getPMLabour() {
		return pmLabour;
	}
	
	public int[] getCMLabour() {
		return cmLabour;
	}
	
	public static long notZero(double input)
	{
		long output = (long) input;
		if(output == 0)
			output = 1;
		return output;
	}
}
