package org.isw;

import java.util.ArrayList;
import java.util.Arrays;

public class LabourAvailability {
	/*
	 * Manages number of available labours at any point of time in a shift
	 */
	ArrayList<MaintenanceTuple> timeline;
	
	public LabourAvailability(int[] maxLabour, long shiftDuration)
	{
		timeline = new ArrayList<MaintenanceTuple>();
		MaintenanceTuple tuple = new MaintenanceTuple(0, shiftDuration*2, maxLabour);
		timeline.add(tuple);
	}
	
	public int[] getCurrent(long time)
	{
		for(MaintenanceTuple m: timeline)
		{
			if(m.start<=time && m.end>time)
				return m.labour;
		}
		return null;
	}
	
	public boolean equals(int[] labour1, int[] labour2)
	{
		if(labour1[0]==labour2[0] && labour1[1]==labour2[1] && labour1[2]==labour2[2])
			return true;
		else
			return false;
	}
	
	public boolean checkAvailability(MaintenanceTuple mtTuple)
	{
		return checkAvailability(mtTuple.start, mtTuple.end, mtTuple.labour);
	}
	
	public boolean checkAvailability(long startTime, long endTime, int[] labour)
	{
		/*
		 * If given labour is available between start and end time
		 */
		
		if(endTime>timeline.get(timeline.size()-1).end)
		{
			// extend the timeline as request is beyond timeline range
			timeline.get(timeline.size()-1).end =  endTime*2;
			
		}
		
		for(int i=0; i<timeline.size(); i++)
		{
			MaintenanceTuple curr = timeline.get(i);
			
			if(curr.end < startTime)
				continue;
			
			else if(curr.start >= endTime)
				return true;
			
			else
			{
				if(!(curr.labour[0]>=labour[0] && curr.labour[1]>=labour[1] && curr.labour[2]>=labour[2]))
					return false;
			}
		}
		return true;
	}
	
	public void employLabour(MaintenanceTuple mtTuple)
	{
		employLabour(mtTuple.start, mtTuple.end, mtTuple.labour);
	}
	
	public void employLabour(long startTime, long endTime, int[] labour)
	{
		/*
		 * Subtract specified amount of labour from available labour between start and end time
		 */
		if(endTime>timeline.get(timeline.size()-1).end)
		{
			// extend the timeline as request is beyond timeline range
			timeline.get(timeline.size()).end *=  endTime;
			
		}
		
		for(int i=0; i<timeline.size(); i++)
		{
			MaintenanceTuple curr = timeline.get(i);
			
			// no overlap
			if(curr.end < startTime)
				continue;
			else if(curr.start >= endTime)
				break;
			
			// if Tuple overlaps
			else if (curr.start >= startTime && curr.end <= endTime)
			{
				// curr is completely contained in (startTime, endTime)
				curr.labour[0] -= labour[0];
				curr.labour[1] -= labour[1];
				curr.labour[2] -= labour[2];
			}
			
			else if (curr.start <= startTime && curr.end >= endTime)
			{
				// (startTime, endTime) is completely contained in curr
				MaintenanceTuple former = new MaintenanceTuple(curr.start, startTime, Arrays.copyOf(curr.labour, 3));
				MaintenanceTuple latter = new MaintenanceTuple(endTime, curr.end, Arrays.copyOf(curr.labour, 3));
				
				timeline.add(i, former);
				i+=2;
				timeline.add(i, latter);
				
				curr.start = startTime;
				curr.end = endTime;
				curr.labour[0] -= labour[0];
				curr.labour[1] -= labour[1];
				curr.labour[2] -= labour[2];
			}
			
			else
			{
				// partial overlap
				if(curr.start < startTime)
				{
					MaintenanceTuple latter = new MaintenanceTuple(startTime, curr.end, Arrays.copyOf(curr.labour,3));
					latter.labour[0] -= labour[0];
					latter.labour[1] -= labour[1];
					latter.labour[2] -= labour[2];
					curr.end = startTime;
					i++;
					timeline.add(i, latter);
				}
				else if(curr.end > endTime)
				{
					MaintenanceTuple latter = new MaintenanceTuple(endTime, curr.end, Arrays.copyOf(curr.labour, 3));
					curr.labour[0] -= labour[0];
					curr.labour[1] -= labour[1];
					curr.labour[2] -= labour[2];
					curr.end = endTime;
					i++;
					timeline.add(i, latter);
				}
			}
		}
	}
	
	/*public long findWhenAvailable(long after, long duration, int[] labour)
	{
		
	}*/
	
	public void print()
	{
		for(MaintenanceTuple t : timeline)
		{
			System.out.format("[%d,%d] - (%d,%d,%d)\n", t.start, t.end,t.labour[0],t.labour[1],t.labour[2]);
		}
	}
	
}
