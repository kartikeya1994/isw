package org.isw.test;

import java.util.Hashtable;



public class ComboTest {

	
	
	public static void main(String[] args) {
		long upper =(long) Math.pow(2, 5*6)-2;
		long num;
		Hashtable<Long, Boolean> hashTable = new Hashtable<Long, Boolean>();
		for(int i=0;i<1000;i++)
		{
			num = (long)(Math.random()*upper+1);
			while(hashTable.containsKey(new Long(num)))
			{
				num = (long)(Math.random()*upper+1);
			}
			System.out.println(num);
			
		}
		
		//for(int i=0;i<4096;i++)
		//	System.out.println(getChromosome(getCombolist(i)));
	}
	
	public static int[] getCombolist(int combo) {
		int combos[] = new int[3];
		for(int i =0;i<3;i++){
			combos[i] = (combo>>(4*i))&(15);
		}
		return combos;
	}
	
	public static int getChromosome(int[] compCombo) {
		int combo = compCombo[compCombo.length-1];
		for(int i = compCombo.length-2; i >=0  ;i--){
			combo = (combo<<4);
			combo |= compCombo[i];
		}
		return combo;
	}

}
