package org.isw.test;


public class PointerTest {
	static int b[];
	public static void main(String args[]){
		int a[] = {1,1,1};
		copy(a);
		System.out.println(b[0]);
		
	}

	private static void copy(int[] a) {
		b = a;
	}
}
