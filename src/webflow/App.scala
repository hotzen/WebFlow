package webflow

import dataflow._
import dataflow.ForkJoinScheduler
import dataflow.io.{Dispatcher, Acceptor, Socket}
import dataflow.io.http._

import dataflow.util.{Logger, LogLevel}

object App {

  Logger.DefaultLevel = LogLevel.Trace
  
  def main(args: Array[String]): Unit = {
    serve
  }
  
  def serve = {
    implicit val scheduler = new ForkJoinScheduler
    implicit val dispatcher = Dispatcher.start
    
    val a = new Acceptor
    a.bind("localhost", 8080)
        
    println("serving")
    val f1 = flow {
      a.accept.sawait
    }
    
    flow {
      a.connections.foreach(socket => flow {
        println("new socket: " + socket)
        
        val http = new HttpProcessor(socket)
        
        http.requests.foreach(request => {
          println("REQUEST:\n" + request)
          
          val headers = List[HttpHeader](
            HttpContentType.HTML.toHeader("UTF-8"),
            HttpHeader("X-Foo", "Bar"),
            HttpHeader("X-HTTPD", "WebFlow2010")
          )
          
          val response = HttpResponse(HttpResponseStatus.OK, headers)
          response.addBody("<h1>HELLO WORLD</h1>")
          
          socket.write << Socket.ByteBufferToArray(response.toBytes)
          //socket.close
        })
      })
    }
        
    f1.await
    dispatcher.shutdown
    scheduler.shutdown.await
  }
}