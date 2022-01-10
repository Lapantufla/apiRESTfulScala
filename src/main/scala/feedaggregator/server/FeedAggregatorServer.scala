package feedaggregator.server
//new
import akka.util.Timeout
import akka.pattern.ask
import akka.actor.{ Actor, ActorSystem, Props, ActorLogging}
//
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import spray.json.DefaultJsonProtocol._
import scala.io.StdIn
import scala.concurrent.Await
import scala.concurrent.duration._

//new
import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import scala.collection.mutable._
//

import principalActor.pA._
import secondActor.sA._
import hellowActor._
import sinceActor.sA._
import xmlActor.xA._

object FeedAggregatorServer {
  final case class FeedItem(title: String, link: String, description: Option[String], pubDate: String)
  final case class FeedInfo(title: String, description: Option[String], items: List[FeedItem])
  implicit val ec :scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
  // Needed for Unmarshalling
  // adjusting number of arguments
  implicit val feedItem = jsonFormat4(FeedItem)
  implicit val feedInfo = jsonFormat3(FeedInfo)

  // TODO: This function needs to be moved to the right place
  /*
  def syncRequest(path: String): Either[Throwable, xml.Elem] = {
    import dispatch._, Defaults._
    val rss = dispatch.Http.default(dispatch.url(path) OK dispatch.as.xml.Elem).either
    Await.result(rss, 15.seconds)
  }*/
 
  def main(args: Array[String]) {
    implicit val system = akka.actor.ActorSystem("system")
    // needed for the future flatMap/onComplete in the end
    val principalActor = system.actorOf(Props[PrincipalActor])
    val secondActor = system.actorOf(Props[SecondActor])
    implicit val executionContext = system.dispatcher   
    //aca voy a guardar las subscripciones ya parseadas
    var listfeeds: List[xml.Elem] = List()
    
    val route = concat (
      path("") {
        get {
          implicit val timeout: Timeout = 5.seconds
          val futureHello: Future[String] = (principalActor ? HelloWorld).mapTo[String]
          
          onComplete(futureHello){
            case Success(hello) => 
              complete(hello) 
            case Failure(err) => 
              complete(StatusCodes.BadRequest -> s"Bad Request: ${err.getMessage}\n") 
          }
        }
      }, 
      path("feed") {
        get {
          parameter("url".as[String]) { (url) =>
            parameter("since". ?){ (since) =>
              implicit val timeout: Timeout = 5.seconds
              val futurefeed: Future[FeedInfo] = 
                (principalActor ? GetFeed(url,since)).mapTo[FeedInfo]    
              
              onComplete(futurefeed){
                case Success(value) => 
                  complete(value)
                case Failure(err) => 
                  complete(StatusCodes.BadRequest -> s"Bad Request: ${err.getMessage}\n")
              }
            }
          }  
        } 
      },
      path("subscribe"){
        post{
          parameter("url".as[String]) { (url) =>
          //implicit val timeout: Timeout = 5.seconds
          implicit val timeout: Timeout = 5.seconds
          val futuresubscribe: Future[xml.Elem] = 
            (secondActor ? SubscribeUrl(url)).mapTo[xml.Elem]    
            
          onComplete(futuresubscribe){
            case Success(value: xml.Elem) => {
              //anado a la lista una subscripcion
              listfeeds = listfeeds.++(List(value))
              complete(StatusCodes.OK)
            }
            case Failure(err) => 
              complete(StatusCodes.BadRequest -> s"Bad Request: ${err.getMessage}\n")
            }
          }
         }
      },
      path("feeds"){
        get{
          //me fijo si hay subscripciones a urls
          if(listfeeds.isEmpty){
            complete(StatusCodes.BadRequest -> "No estas subscrito a ninguna pagina\n")
          }else{
            parameter("since". ?){ (since) => 
              {
                implicit val timeout: Timeout = 5.seconds
                val futurefeed: Future[List[FeedInfo]] = 
                  (secondActor ? GetFeedtoxml(listfeeds,since)).mapTo[List[FeedInfo]]    
                
                onComplete(futurefeed){
                  case Success(value: List[FeedInfo]) => {
                    complete(value)
                  }
                  case Failure(err) => 
                    complete(StatusCodes.BadRequest -> s"Bad Request: ${err.getMessage}\n")
                }
              }   
            }
          }  
        }  
      } 
    )
    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)
    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}

        



        
