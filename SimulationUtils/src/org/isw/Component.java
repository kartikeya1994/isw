package org.isw;

import java.io.Serializable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.apache.commons.math3.random.GaussianRandomGenerator;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;


public class Component implements Serializable{
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
	public transient StringProperty compNameP;
	public transient DoubleProperty initAgeP;
	public transient BooleanProperty active;
	public transient DoubleProperty pmMuRepP;
	public transient DoubleProperty pmSigmaRepP;
	public transient DoubleProperty pmMuSuppP;
	public transient DoubleProperty pmSigmaSuppP;
	public transient DoubleProperty pmRFP;
	public transient DoubleProperty pmCostSpareP;
	public transient DoubleProperty pmCostOtherP;
	//CM
	// TOF
	public transient DoubleProperty cmEtaP;
	public transient DoubleProperty cmBetaP;
	// TTR
	public transient DoubleProperty cmMuRepP;
	public transient DoubleProperty cmSigmaRepP;
	public transient DoubleProperty cmMuSuppP;
	public transient DoubleProperty cmSigmaSuppP;
	public transient DoubleProperty cmRFP;
	public transient DoubleProperty cmCostSpareP;
	public transient DoubleProperty cmCostOtherP;

	public Component(Component component) {
		compName = component.compName;
		initAge = component.initAge;
		cmEta = component.cmEta;
		cmBeta = component.cmBeta;
		cmMuRep = component.cmMuRep;
		cmSigmaRep = component.cmSigmaRep;
		cmMuSupp = component.cmMuSupp; 
		cmSigmaSupp = component.cmSigmaSupp;
		cmRF = component.cmRF;
		cmCostSpare = component.cmCostSpare;
		cmCostOther = component.cmCostOther;
		pmMuRep = component.pmMuRep;
		pmSigmaRep = component.pmSigmaRep;
		pmMuSupp = component.pmMuSupp;
		pmSigmaSupp = component.pmSigmaSupp;
		pmRF = component.pmRF;
		pmCostSpare = component.pmCostSpare;
		pmCostOther = component.pmCostOther;
		pmLabour = component.pmLabour;
		cmLabour = component.cmLabour;
		labourCost = component.labourCost;
	}
	public Component(){
		
	};
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
		return (Math.pow(b-((Math.log(1-Math.random())/a)),(1/p)))-t0;
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

	public void initProps(int i) {
		if(i<4)
			active = new SimpleBooleanProperty(true);
		else
			active = new SimpleBooleanProperty(false);
		compNameP = new SimpleStringProperty(compName);
		initAgeP = new SimpleDoubleProperty(initAge);
		cmEtaP = new SimpleDoubleProperty(cmEta);
		cmBetaP = new SimpleDoubleProperty(cmBeta);
		cmMuRepP = new SimpleDoubleProperty(cmMuRep);
		cmSigmaRepP = new SimpleDoubleProperty(cmSigmaRep);
		cmMuSuppP = new SimpleDoubleProperty(cmMuSupp);
		cmSigmaSuppP = new SimpleDoubleProperty(cmSigmaSupp);
		cmRFP = new SimpleDoubleProperty(cmRF);
		cmCostSpareP = new SimpleDoubleProperty(cmCostSpare);
		cmCostOtherP = new SimpleDoubleProperty(cmCostOther);
	
		pmMuRepP = new SimpleDoubleProperty(pmMuRep);
		pmSigmaRepP = new SimpleDoubleProperty(pmSigmaRep);
		pmMuSuppP = new SimpleDoubleProperty(pmMuSupp);
		pmSigmaSuppP = new SimpleDoubleProperty(pmSigmaSupp);
		pmRFP = new SimpleDoubleProperty(pmRF);
		pmCostSpareP = new SimpleDoubleProperty(pmCostSpare);
		pmCostOtherP = new SimpleDoubleProperty(pmCostOther);
		
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
