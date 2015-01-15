
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import akka.actor._
import scala.collection.mutable.ArrayBuffer
import akka.routing.RoundRobinRouter
import java.security.MessageDigest
import scala.util.control.Breaks
import scala.util.Random
import javax.xml.bind.DatatypeConverter
import java.net.InetAddress

sealed trait Msg
case class LocalMessage(ab:ArrayBuffer[String]) extends Msg
case class RemoteMessage(ab:ArrayBuffer[String]) extends Msg
case class AllocateWork(mvalue:Int) extends Msg
case class StartMaster(mvalue: Int) extends Msg
case class StartWorker(begin: Long,size:Long,mvalue :Int) extends Msg
case class ReplyToMaster(output: ArrayBuffer[String] ) extends Msg
case class SHAHashVal(input: ArrayBuffer[String]) extends Msg
case class mineBitCoins(startN: Long,size:Long, mvalue : Int) extends Msg
case class BitClient(str: String) 


object Server {
  def main(args: Array[String]) {
    
	val localhost = InetAddress.getLocalHost
    val hostname = InetAddress.getLocalHost.getHostName
    val config = ConfigFactory.parseString(
      """ 
     akka{ 
    		actor{ 
    			provider = "akka.remote.RemoteActorRefProvider" 
    		} 
    		remote{ 
                enabled-transports = ["akka.remote.netty.tcp"] 
            netty.tcp{ 
    			hostname = "192.168.1.120" 
    			port = 5150
    		} 
      }      
    }""")
    
    
    val mvalue = Integer.parseInt(args(0))
    
    val system = ActorSystem("HelloRemoteSystem", ConfigFactory.load(config))
    val remoteActor = system.actorOf(Props(new RemoteActor(mvalue)), name = "RemoteActor")
    remoteActor ! "The Server is Up & Running"
    
     val listener = system.actorOf(Props[Listener], name = "listener")

    // create the master
    val master = system.actorOf(Props(new Master(8, 1000,50, listener)), name = "master")

    // start the calculation
    master ! StartMaster(mvalue)
    
  }
}

class Listener extends Actor {
  def receive = {
    case SHAHashVal(input) =>
      {
        println("Bitcoins found on Server :")
        for(i <- 0 until input.length){
           if(i%2==0) print(input(i) +"    :  ")
           else println(input(i))
        }  
      }
   // context.system.shutdown()
  }
}

class RemoteActor(mvalue : Int) extends Actor {
  def receive = {
    case "GETWORK" => {
      println("Client Request for Work found")
      sender ! AllocateWork(mvalue)
    }
    case RemoteMessage(ab) => {
      println("Bitcoins Recevied from client")
      println("Length   :  " + (ab.length/2))
      for (i <- 0 until ab.length) {
        if (i % 2 == 0) print(ab(i) + "   :  ")
        else println(ab(i))
      }
    }    
    case BitClient(str) => println("BitCoin From Client "+str)
    
    case _ => {
      println("No request from client.")      
    }

  }
} 


 
class Master(WorkersCount: Int, initial: Long,size: Long,listener: ActorRef) extends Actor {

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
 			    
 	case "WorkFinished"=>
 	  //println("Numbed of Chunks"+numofChunks+" Acknowlegment"+Acknwlgmnt)
 			  Acknwlgmnt = Acknwlgmnt + 1
				if (Acknwlgmnt == numofChunks) {
				   println("Number of bitcoins is "+total)
					//println("Inputsize "+initial+" to "+startN)
					context.system.shutdown()
					sys.exit() 
 			  
				}
    case StartMaster(mvalue) =>
     val WorkerRouter = context.actorOf(Props[Worker].withRouter(RoundRobinRouter(WorkersCount)), name = "router")
	 //println("After Worker Router");
	 			while(System.currentTimeMillis()<(starttime+1000)){
		 			WorkerRouter ! mineBitCoins(startN,size,mvalue)
		 				numofChunks=numofChunks+1
		 			startN=startN+size+1
		 			}
	 			self!"WorkFinished"
	 			
    
  }
}


class Worker extends Actor {  
  def receive = {
    case mineBitCoins(startN: Long,size:Long, mvalue : Int) =>
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
                    	//println(hex)
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
                    	  	//println(" text"+hashedMsgHex
                    	  		{println("BitCoin From Server "+text+ "	" +hex )}
                    	  	  
                    	  	sender!"bitcoinfound"
                    	  	}
                    	  		               		
                    	  }
                    	num=num+1
                    } 
			//result
			sender ! "WorkFinished"
  			}
    
		}


