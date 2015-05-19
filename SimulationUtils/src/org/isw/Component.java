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
	public double p1;
	public double p2;
	public double p3;
	//CM
	// TOF
	public double cmEta;
	public double cmBeta;
	// TTR
	public double cmMu;
	public double cmSigma;
	public double cmRF;
	public double cmCost;
	//PM
	// TTR
	public double pmMu;
	public double pmSigma;
	public double pmRF;
	public double pmCost;
	public double pmFixedCost;
	
	public double compCost;
	public double initAge;

	public double getPMTTR(){
		return normalRandom(pmMu,pmSigma);
	}
	
	public double getCMTTR(){
		return normalRandom(cmMu,cmSigma);
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

	public double getCMCost() {
		return cmCost;
	}

	public double getPMCost() {
		return pmCost;
	}

	public double getCompCost() {
		return compCost;
	}
	public double getPMFixedCost(){
		return pmFixedCost;
	}
}
