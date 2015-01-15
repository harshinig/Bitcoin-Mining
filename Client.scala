import akka.actor._
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import scala.collection.mutable.ArrayBuffer
import akka.routing.RoundRobinRouter
import java.security.MessageDigest
import scala.util.control.Breaks
import scala.util.Random


sealed trait Msg
case class ClientResult(totalClient:Long)
case class LocalMessage(ab:ArrayBuffer[String]) extends Msg
case class RemoteMessage(ab:ArrayBuffer[String]) extends Msg
case class AllocateWork(mvalue:Int) extends Msg
case class StartMaster(mvalue: Int) extends Msg
case class BitClient(str:String)
case class StartWorker(begin: Long,size: Long,mvalue :Int) extends Msg
case class ReplyToMaster(output: ArrayBuffer[String] ) extends Msg
case class SHAHashVal(input: ArrayBuffer[String]) extends Msg
object Client {

  def main(args: Array[String]) {
    val hostname = InetAddress.getLocalHost.getHostName
    val config = ConfigFactory.parseString(
      """akka{
		  		actor{
		  			provider = "akka.remote.RemoteActorRefProvider"
		  		}
		  		remote{
                   enabled-transports = ["akka.remote.netty.tcp"]
		  			netty.tcp{
						hostname = "127.0.0.1"
						port =0
					}
				}     
    	}""")

    implicit val system = ActorSystem("LocalSystem", ConfigFactory.load(config))
  
    val proxyActor2 = system.actorOf(Props(new ProxyActor2(args(0))), name = "proxyActor2") // the local actor
       

    
    val master =  system.actorOf(Props(new Master(8,1,50,proxyActor2)),name = "master")

    val proxyActor = system.actorOf(Props(new ProxyActor(args(0),master)), name = "proxyActor") // the local actor
    proxyActor ! "GETWORKFROMSERVER" // start the action
    
  }
}

class ProxyActor(ip: String , master:ActorRef) extends Actor {
  println("akka.tcp://HelloRemoteSystem@" + ip + ":5150/user/RemoteActor")
  // create the remote actor
  val remote = context.actorFor("akka.tcp://HelloRemoteSystem@" + ip + ":5150/user/RemoteActor")

  def receive = {
    case "GETWORKFROMSERVER" =>
      remote ! "GETWORK"
    case AllocateWork(mvalue) =>
       println("recd from server :" + mvalue)
       master ! StartMaster(mvalue)
    case SHAHashVal(input) =>
       remote ! RemoteMessage(input)
    case msg: String =>
      println(s"LocalActor received message and the k Value is: '$msg'")
  }
}

class ProxyActor2(ip: String) extends Actor {
  println("akka.tcp://HelloRemoteSystem@" + ip + ":5150/user/RemoteActor")
  // create the remote actor
  val remote = context.actorFor("akka.tcp://HelloRemoteSystem@" + ip + ":5150/user/RemoteActor")

  def receive = {
   
    case LocalMessage(input) =>
       println("Sending to Remote")
       remote ! RemoteMessage(input)
       println("Sent to Remote") 
    case total:Long=>
     // println("Total Bitcoins from Client"+total )
     // remote!ClientResult(total)
    case BitClient(str)=>//println(" we are here")
      //println(str)
      remote!BitClient(str)
    case _ =>
      println("Unknown message in Proxyactor")
   
  }
}


class Master(WorkersCount: Int, initial: Long,size: Long,proxyActor2:ActorRef) extends Actor {

  var result: ArrayBuffer[String] = new ArrayBuffer[String]()
  var nrOfResults: Int = _
  val starttime : Long = System.currentTimeMillis
  var startN=initial
  var numofChunks:Int=0
  var total:Long=0
  var Acknwlgmnt:Int=0
  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(WorkersCount)), name = "workerRouter")

  def receive = {
    case "bitcoinfound"=> 			 
 			    total=total+1
    case BitClient(str)=>
      println(str)
      proxyActor2!BitClient(str)		    
 	case "WorkFinished"=>
 			  Acknwlgmnt = Acknwlgmnt + 1
				if (Acknwlgmnt == numofChunks) {
				 
				   proxyActor2!total
				 //context.system.shutdown()
					//sys.exit()
				}
 			 
    case StartMaster(mvalue) =>
     val WorkerRouter = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(WorkersCount)), name = "router")
	 println("After Worker Router");
             startN=10000000
	 			while(System.currentTimeMillis()<(starttime+4000)){
		 			WorkerRouter ! StartWorker(startN,size,mvalue)
		 				numofChunks=numofChunks+1
		 			startN=startN+size+1
		 			}}

}


class Worker extends Actor {  
  def receive = {
    case StartWorker(startN,size,mvalue) =>  
  
  
		var count=0
		var result = new ArrayBuffer[String]()
		val secureHash256 = java.security.MessageDigest.getInstance("SHA-256")
		var num:Long=startN	
		while(num<=startN+size) {			  		 	
                    	val md= MessageDigest.getInstance("SHA-256")
                    	md.reset();
                    	val text:String="ritu.sharmanitr"+num;
                    	//println(text)
                    	md.update(text.getBytes("UTF-8"));
                    	val hashedMessage:Array[Byte]=md.digest();
                    	val hashedMsgHex: StringBuffer = new StringBuffer
                    	val hex:String=DatatypeConverter.printHexBinary(hashedMessage);
                    
                    	if (hex.length >3)
                    	  {
                    		hashedMsgHex.append(hex)
                    		var sub:String=""
                    		for(i<-1 to mvalue){
                    		  sub=sub+"0"
                    		}
                    		val initialchar : String = hashedMsgHex.toString.substring(0,mvalue);
                    	  	if (initialchar == sub) 
                    	  	{
                    	  	val str:String=text+"     "+hex
                    	  
                    	  	sender!BitClient(str)
                    	  	
                    	  	}
                    	  		               		
                    	  }
                    	num=num+1
                     
			sender! "WorkFinished"
			
  			}
    
		}  

}
