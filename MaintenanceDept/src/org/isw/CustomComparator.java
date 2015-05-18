package org.isw;

import java.util.Comparator;

class CustomComparator implements Comparator<SimulationResult>
{
	//sort such that lower pmOpportunity, higher cost and lower PM time
	@Override
	public int compare(SimulationResult a, SimulationResult b) 
	{
		if(a.t == b.t)
		{
			if(a.cost == b.cost)
			{
				return signOf(a.pmAvgTime - b.pmAvgTime);
			}
			else
				return signOf(b.cost - a.cost);
		}
		else
			return (int)(a.t - b.t);
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