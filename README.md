************************
To run event-based jars: 
************************
java -jar IndustrySmartware.jar
java -jar Scheduler.jar 3 500
java -jar MaintenanceDept.jar
java -jar MachineSimulator.jar MA MUTE

***********
Other notes
***********

ISW, Maintenance: No parameters
Scheduler: No. of jobs, seconds multiplication
Machine: MA/BF, MUTE

order of args must be maintained. 
MA/BF arg is not functional anymore, but required if MUTE arg needs to be used. Use random MA/BF for mute.

IMPORTANT: RUN IN THE FOLLOWING ORDER (Bakshi - do not switch scheduler and maintenance)
1. IndustrySmartware
2. Scheduler
3. MaintenanceDept
4. Machines

*************
To run docker
*************
sudo docker run -it --rm -v /home/kartikeya/btp/isw/isw/out:/tmp/isw  isw
sudo docker run -it --rm -v /home/kartikeya:/tmp/isw  isw
sudo route add -net 224.0.0.0/4 dev docker0

****************
To set and unset git proxy
****************
git config --global http.proxy http://cse1200114:passwordx@webproxy.indore.iiti.ac.in:8080
git config --global --unset http.proxy






