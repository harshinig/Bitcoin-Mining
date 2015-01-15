# Bitcoin-Mining
Instructions:
There are 2 .scala files-> Client.scala and Server.scala
Step1.Upload Client.scala and Server.scala on different machines and change the hostname in the val config of Server.scala to IP address of that machine
Step2.Compile both files as follows go to the package folder
sbt compile
Step2a.To execute giving number of zeroes as input(n),execute server as follows
sbt "run n"
Step2b. To execute giving IP address (ip of server) as input, execute both Client.scala and Server.scala as follows
sbt "run n" //for running on server machine – n number of zero sbt "run ip" //for running on client machine – ip servers ip
Size of Work Unit: 50
We determined work size unit by trying different sizes and verifying the efficiency of workers in Computing bitcoins.Smaller work sizes than this were not efficient as too many messages were being sent and lesser work is done because of the overhead.
Number of Workers used: 8 per machine Largest number of working machines: 3
