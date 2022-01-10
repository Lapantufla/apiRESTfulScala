package hellowActor
import akka.actor.{ Actor, ActorSystem, Props, ActorLogging}

case object HelloWorld
  //HELLO ACTOR
class HelloActor extends Actor with ActorLogging{
  //actor encargado de printear el msg hello world  
  def receive = {
      
    case HelloWorld => sender ! "Hello, World!"
  }
}