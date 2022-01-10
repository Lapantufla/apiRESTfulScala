package xmlActor

import akka.util.Timeout
import akka.actor.{ Actor, ActorSystem, Props, ActorLogging}

import scala.util.{Success, Failure}
import scala.concurrent.Future

object xA{
  
  implicit val ec :scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  case class Getxml(url:String)
  //XML ACTOR
  class XmlActor extends Actor with ActorLogging {
    //actor encargado de obtener el xml de la url  
    def receive = {
      
      case Getxml(path) => {
        //guardo esto para saber a quien responderle
        val zender = sender
        val rss = dispatch.Http.default(dispatch.url(path) OK dispatch.as.xml.Elem)
        //me fijo que el futuro del rss haya terminado bien
        rss.onComplete{
          case Success(result) => 
            zender ! result
          case Failure(ex) => 
            zender ! ex
        }
      } 
    } 
  }
}
  