package secondActor

import functionAux.functionA._
import xmlActor.xA._
import sinceActor.sA._
import feedaggregator.server.FeedAggregatorServer._

import akka.util.Timeout
import akka.pattern.ask
import akka.actor.{ Actor, ActorSystem, Props, ActorLogging}

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.util.{Success, Failure}
import scala.concurrent.Future
import java.text.SimpleDateFormat
import java.util.Date

object  sA{
  
  implicit val ec :scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  case class GetFeedtoxml(listxmlparsed: List[xml.Elem],since: Option[String])
  case class SubscribeUrl(url:String)
  //SECOND ACTOR
  class SecondActor extends Actor with ActorLogging{
    implicit val timeout: Timeout = 5.seconds
    //actor encargado de /subscribe y /feeds
    def receive = {
      
      case SubscribeUrl(url) => {
        val xmlActor = context.actorOf(Props[XmlActor])
        val zender1 = sender
        //parsear el xml correspondiente a la url a la cual me quiero subscribir
        val xmlresponse:Future[xml.Elem] = ((xmlActor ? Getxml(url)).mapTo[xml.Elem])  
        xmlresponse.onComplete{
          case Success(result:xml.Elem) =>    
            zender1 ! result
            zender1 ! "Ok"   
          case Failure(ex) => 
            zender1 ! ex
        }
      }
      case GetFeedtoxml(listxmlparsed,since) => {
        //como el xml ya esta parseado falta parsear el since x eso creo este actor
        val sinceActor = context.actorOf(Props[SinceActor])
        val zender1 = sender
        
        val feed = for{
          x1 <- (sinceActor ? GetSince(since)).mapTo[Option[Date]]
          //una vez ya parseado el since puedo filtrarlo
          x2 <- Future{
            val listFeedinfo = parsedlistfeedinfo(listxmlparsed)
            listFeedinfo.map(c => FeedInfo(c.title, c.description, c.items.filter(b => testDate(b.pubDate, x1))))
          } 
        }yield(x2)
        //checkeo que este todo bien
        feed.onComplete{
          case Success(result:List[FeedInfo]) =>    
            zender1 ! result 
          case Failure(ex) => 
            zender1 ! ex
        }
      }
    }
  }
}
  