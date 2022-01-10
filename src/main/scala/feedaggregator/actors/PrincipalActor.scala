package principalActor

import functionAux.functionA._
import hellowActor._
import sinceActor.sA._
import xmlActor.xA._
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

object  pA{
  
  implicit val ec :scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  case class GetFeed(url: String,since: Option[String])
  //PRINCIPAL ACTOR
  class PrincipalActor extends Actor{
    implicit val timeout: Timeout = 5.seconds
  //actor encargado de /feed y /""
    def receive = {  
      
      case HelloWorld => {
        //actor para hacer el hello
        val helloActor = context.actorOf(Props[HelloActor])
        val zender1 = sender
        //mando msg a helloactor para que imprima msg
        val futhello: Future[String] = (helloActor ? HelloWorld).mapTo[String]
        futhello.onComplete{
          case Success(result:String) => 
            zender1 ! result 
          case Failure(ex) => 
        }
      }
      case GetFeed(url,since) => {
        //actor para parsear xml
        val xmlActor = context.actorOf(Props[XmlActor])
        //actor para parseaer since y convertir formato
        val sinceActor = context.actorOf(Props[SinceActor])
        val zender1 = sender
        //mando msjs a xmlactor y sinceactor para que parseen el xml y el since(poniendolo en formato)
        val sinceresponse:Future[Option[Date]] = ((sinceActor ? GetSince(since)).mapTo[Option[Date]])
        val xmlresponse:Future[xml.Elem] = ((xmlActor ? Getxml(url)).mapTo[xml.Elem])
        
        val feed = for{
          x1 <- sinceresponse
          x2 <- xmlresponse
          //una vez parseado el since y parseado el xml obtengo el feedinfo filtrandolo con la funcion testdate
          x3 <- Future{
            val feedinfo = parsedfeedinfo(x2)
            FeedInfo(feedinfo.title, feedinfo.description,parsedfeedinfo(x2).items.filter(c => testDate(c.pubDate, x1))) 
            } 
          }yield(x3)
        //checkeo que este todo bien
        feed.onComplete{
          case Success(result:FeedInfo) =>    
            zender1 ! result 
          case Failure(ex) => 
            zender1 ! ex
        }
      }
    }
  }
}  
  