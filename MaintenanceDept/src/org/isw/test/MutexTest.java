package org.isw.test;

public class MutexTest {

	static int chocolates=0;

	public static void main(String args[])
	{
		final Object lock = new Object();
		Producer p = new Producer(lock);
		Consumer c1 = new Consumer(lock,1);
		Consumer c2 = new Consumer(lock,2);

		Thread t1 = new Thread(p);
		Thread t2 = new Thread(c1);
		Thread t3 = new Thread(c2);

		t1.start();
		t2.start();
		t3.start();
	}
}

class Consumer implements Runnable{

	Object lock;
	int id;
	
	public Consumer(Object lock, int id)
	{
		this.lock = lock;
		this.id = id;
	}
	public void run()
	{
		while(true){


			synchronized(lock)
			{
				System.out.println("CONSUMER "+id);
				System.out.println("Old Chocolates: "+MutexTest.chocolates);
				MutexTest.chocolates -= 1;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				System.out.println("New Chocolates: "+MutexTest.chocolates);
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
	}
}

class Producer implements Runnable{

	Object lock;
	public Producer(Object lock)
	{
		this.lock = lock;
	}
	public void run()
	{
		while(true)
		{
			synchronized(lock)
			{
				System.out.println("\t\tPRODUCER");
				System.out.println("\t\tOld Chocolates: "+MutexTest.chocolates);
				MutexTest.chocolates += 5;
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
				System.out.println("\t\tNew Chocolates: "+MutexTest.chocolates);
			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
	}
}
