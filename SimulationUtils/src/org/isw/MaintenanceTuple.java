package org.isw;

public class MaintenanceTuple
{
	public long start;
	public long end;
	public int[] labour;
	
	public MaintenanceTuple(long start, long end, int[] labour)
	{
		this.start = start;
		this.end = end;
		this.labour = labour;
	}
	
	public MaintenanceTuple()
	{
		
	}
	
	public MaintenanceTuple(int start)
	{
		this.start = -1;
	}
}
