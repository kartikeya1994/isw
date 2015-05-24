package org.isw;

import java.util.Comparator;

public class CustomComparator implements Comparator<SimulationResult> {

	@Override
	public int compare(SimulationResult a, SimulationResult b) {
		if(a.t == b.t)
		{
			if(a.cost == b.cost)
			{
				return signOf(a.pmAvgTime - b.pmAvgTime);
			}
			else
				return signOf(a.cost - b.cost);
		}
		else
			return (int)(b.t - a.t);
		
	}
	public int signOf(double a)
	{
		if(a>0)
			return 1;
		else if(a<0)
			return -1;
		else
			return 0;
	}

}
