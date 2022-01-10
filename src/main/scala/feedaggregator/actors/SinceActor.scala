package sinceActor

import akka.util.Timeout
import akka.actor.{ Actor, ActorSystem, Props, ActorLogging}

import scala.util.{Success, Failure}
import scala.concurrent.Future
import java.text.SimpleDateFormat
import java.util.Date

object  sA{
  
  case class GetSince(since:Option[String])
  implicit val ec :scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  //SINCE ACTOR
  class SinceActor extends Actor with ActorLogging{
    //actor encargado de parsear el since  y ponerle el formato pertinente
    def receive = {
      
      case GetSince(since) => { 
        val zender = sender
        since match{
          case Some(value) => {
            if(value == "Empty"){
              zender ! None
            }else{
              val sincefut:Future[Option[Date]] = Future{parseS(value)}
              sincefut.onComplete{
                case Success(date) => 
                  zender ! date
                case Failure(ex) =>
                  zender ! ex
              }
            }
          }
          case None => {
            zender ! None
          }
        }
      }
    }
    def parseS(since: String): Option[Date] = {
      val format =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
      format.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))  
      Some(format.parse(since))
    }
  }
}  
  