package functionAux

import feedaggregator.server.FeedAggregatorServer._
import scala.util.{Try, Success, Failure}
import scala.concurrent.Future
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object functionA{
    //funcion auxiliar para comparar dates
    def testDate(date1: String, dsince:Option[Date]): Boolean = {  
    dsince match {
      case Some(value: Date) => {  
        // le saco la coma
        val newstring = date1.replaceAll(",", "")
        val format = 
          new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH) 
      //seteo la zona horaria a GMT
        format.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
        val date3 = format.parse(newstring)
        (date3.compareTo(value) < 0)  
      }
      case None => 
        true
    }    
  }
  //funcion para hacer un feedinfo mediante un xml
  def parsedfeedinfo (xmlparsed : xml.Elem): FeedInfo = {
    FeedInfo(
      ((xmlparsed \ "channel") \ "title").headOption.map(_.text).get,
      ((xmlparsed \ "channel") \ "description").headOption.map(_.text),
      ((xmlparsed \ "channel") \\ "item").toList.map(item =>
        FeedItem((item \ "title").headOption.map(_.text).get,
        (item \ "link").headOption.map(_.text).get,
        (item \ "description").headOption.map(_.text), 
        (item \ "pubDate").headOption.map(_.text).get)
      ).toList
    )   
  }        
  //separe estas funciones para que no sea tan complicado el codigo si quiero obtener un solo feedinfo en caso de que quiera una lista uso :
  def parsedlistfeedinfo (xmlparsed: List[xml.Elem]): List[FeedInfo] ={
    xmlparsed.map(c => FeedInfo(
      ((c \ "channel") \ "title").headOption.map(_.text).get,
      ((c \ "channel") \ "description").headOption.map(_.text),
      ((c \ "channel") \\ "item").toList.map(item =>
        FeedItem((item \ "title").headOption.map(_.text).get,
        (item \ "link").headOption.map(_.text).get,
        (item \ "description").headOption.map(_.text), 
        (item \ "pubDate").headOption.map(_.text).get)
      ).toList
      )
    )
  }
}
