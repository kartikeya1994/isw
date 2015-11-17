Sending object over TCP: http://stackoverflow.com/questions/5957790/how-do-you-send-a-user-defined-class-object-over-a-tcp-ip-network-connection-in
How TCP streams in java work: http://www.javaworld.com/article/2077322/core-java/core-java-sockets-programming-in-java-a-tutorial.html
Simple TCP client server example: https://systembash.com/a-simple-java-tcp-server-and-tcp-client/
Thread Pool - https://docs.oracle.com/javase/tutorial/essential/concurrency/pools.html

--------------------
MAINTENANCE DEPT
--------------------
1. Get machine list - main thread
2. Query each machine for IFs and schedule- thread pool
3. Listen for breakdowns, stop ongoing PM, perform CM, resume interrupted PM - BreakdownManagerThread
4. Incorporate PM into schedule - main thread
5. Convey PM incorporated schedule to all machines - thread pool

--------------------
MACHINE
--------------------
1. Setup(parse Excel data sheet), connect to SchedulingDept - one time - main thread
2. receive sans PM schedule, calculate IFs - main thread
3. convey IFs to Maintenance Dept, listen for PM incorporated schedule - main thread
4. Report current status - StatusThread

While running a job, if it is a PM job:
1. Before running job wait for Maintenance Dept to arrive.
2. While running job listen for PM interrupt due to breakdown of some other machine, listen for PM resume if interrupt occurs.

if it is a normal job: 
1. Simulate breakdown if any.

--------------------
SCHEDULING DEPT
--------------------
1. Listen for joining machines - WelcomeThread
2. Query machines for pending jobs - thread pool
3. Create job schedule - main thread
4. Send schedule to all machines - thread pool
5. Listen and wait till all machines report that 8 hours are over - main thread

