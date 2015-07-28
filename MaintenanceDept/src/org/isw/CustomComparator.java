package org.isw;

import java.util.Comparator;

public class CustomComparator implements Comparator<SimulationResult> {

	@Override
	public int compare(SimulationResult a, SimulationResult b) {
		int i = Double.compare(a.t, b.t);
		if(i!=0) return i;
		i = Double.compare(b.cost, a.cost);
		if(i!=0) return i;
		i = Integer.compare(a.pmLabourCount(), b.pmLabourCount());
		if(i!=0) return i;
		return Double.compare(a.pmAvgTime, b.pmAvgTime);		
	}


}
