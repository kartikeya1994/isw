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
	private static final long serialVersionUID = 1L;
	private String compName;
	private double compCost;
	private double initAge;
	private double p1;
	private double p2;
	private double p3;
	//CM
	// TOF
	private double cmEta;
	private double cmBeta;
	// TTR
	private double cmMu;
	private double cmSigma;
	private double cmRF;
	private double cmCost;
	//PM
	// TTR
	private double pmMu;
	private double pmSigma;
	private double pmRF;
	private double pmCost;
	private double pmFixedCost;

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
