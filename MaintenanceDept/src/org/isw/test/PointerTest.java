package org.isw.test;

import java.util.ArrayList;

public class PointerTest {
	
	public static void main(String args[]){
		ArrayList<LOL> al = new ArrayList<LOL>();
		al.add(new LOL(1));
		al.add(new LOL(2));
		al.add(new LOL(3));
		al.add(new LOL(4));
		al.add(new LOL(5));
		al.add(new LOL(6));
		
		for(int i=0;i<al.size();i++)
		{
			if(al.get(i).a % 2 == 0)
			{
				al.remove(i);
				i--;
			}
		}
		for(LOL l:al)
			System.out.println(l.a);
		
	}

}

class LOL
{
	public int a;
	public LOL(int a)
	{
		this.a=a;
	}
}
