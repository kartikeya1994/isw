package org.isw;

import java.util.ArrayList;
import java.util.Arrays;

public class LabourAvailability {
	/*
	 * Manages number of available labours at any point of time in a shift
	 */
	ArrayList<Tuple> timeline;
	
	public LabourAvailability(int[] maxLabour, double shiftDuration)
	{
		timeline = new ArrayList<Tuple>();
		Tuple tuple = new Tuple(0, shiftDuration, maxLabour);
		timeline.add(tuple);
	}
	
	public boolean checkAvailability(double startTime, double endTime, int[] labour)
	{
		/*
		 * If given labour is available between start and end time
		 */
		for(int i=0; i<timeline.size(); i++)
		{
			Tuple curr = timeline.get(i);
			
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
	
	public void employLabour(double startTime, double endTime, int[] labour)
	{
		/*
		 * Subtract specified amount of labour from available labour between start and end time
		 */
		for(int i=0; i<timeline.size(); i++)
		{
			Tuple curr = timeline.get(i);
			
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
				Tuple former = new Tuple(curr.start, startTime, Arrays.copyOf(curr.labour, 3));
				Tuple latter = new Tuple(endTime, curr.end, Arrays.copyOf(curr.labour, 3));
				
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
					Tuple latter = new Tuple(startTime, curr.end, Arrays.copyOf(curr.labour,3));
					latter.labour[0] -= labour[0];
					latter.labour[1] -= labour[1];
					latter.labour[2] -= labour[2];
					curr.end = startTime;
					i++;
					timeline.add(i, latter);
				}
				else if(curr.end > endTime)
				{
					Tuple latter = new Tuple(endTime, curr.end, Arrays.copyOf(curr.labour, 3));
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
	
	public void print()
	{
		for(Tuple t : timeline)
		{
			System.out.format("[%d,%d] - (%d,%d,%d)\n", t.start, t.end,t.labour[0],t.labour[1],t.labour[2]);
		}
	}
	
}

class Tuple
{
	public double start;
	public double end;
	public int[] labour;
	
	public Tuple(double start, double end, int[] labour)
	{
		this.start = start;
		this.end = end;
		this.labour = labour;
	}
}
