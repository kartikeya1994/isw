package org.isw;

import java.util.ArrayList;
import java.util.Comparator;

public class CustomComparator implements Comparator<SimulationResult> {
	
	ArrayList<Component[]> components;
	
	public CustomComparator(ArrayList<Component[]> components)
	{
		this.components = components;
	}

	@Override
	public int compare(SimulationResult a, SimulationResult b) {
		int i = Double.compare(b.cost, a.cost);
		if(i!=0) return i;
		i = Integer.compare(a.getPMLabourCount(components.get(a.id)), b.getPMLabourCount(components.get(a.id)));
		if(i!=0) return i;
		return Double.compare(a.pmAvgTime, b.pmAvgTime);		
	}
	
	
}
