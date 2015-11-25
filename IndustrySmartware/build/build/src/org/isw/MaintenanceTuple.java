package org.isw;

import java.io.Serializable;

public class MaintenanceTuple implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public long start;
	public long end;
	public int[] labour;
	
	public MaintenanceTuple(long start, long end, int[] labour)
	{
		this.start = start;
		this.end = end;
		this.labour = labour.clone();
	}
	
	public MaintenanceTuple()
	{
		
	}
	
	public MaintenanceTuple(int start)
	{
		this.start = start;
	}

	public void print() {
		System.out.format("Start: %d End %d Laboour %d %d %d\n",start,end,labour[0],labour[1],labour[2]);
		
	}
}
