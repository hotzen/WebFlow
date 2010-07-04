package webflow

import dataflow._
import dataflow.ForkJoinScheduler
import dataflow.io.{Dispatcher, Acceptor, Socket}
import dataflow.io.http._

import dataflow.util.{Logger, LogLevel}

object App {

  Logger.DefaultLevel = LogLevel.Info
  
  def main(args: Array[String]): Unit = {
    serve
  }
  
  def serve = {
    implicit val scheduler = new ForkJoinScheduler
    implicit val dispatcher = Dispatcher.start
    
    val a = new Acceptor
    a.bind("localhost", 8080)

    val f1 = flow {
      a.accept.sawait
    }
    
    flow {
      a.connections.foreach(socket => flow {
        println("new socket: " + socket)
        
        val http = new HttpProcessor(socket)
        
        http.requests.foreach(request => {
          println(request.toString)
          
          val headers = List[HttpHeader](
            HttpContentType.HTML.toHeader("UTF-8"),
            HttpHeader("X-HTTPD", "WebFlow2010")
          )
          
          val response = HttpResponse(HttpResponseStatus.OK, headers)
          
          val res = <html>
            <head><title>WebFlow</title></head>
            <body>
            <div>
              <h2>Request-URI</h2>
              <span>{ request.uri.toString }</span>
            </div>
            <div>
              <h2>Request-Headers</h2>
              <ul>
              { request.headers.map(h => {
                <li>
                  <span><strong>{ h.name }</strong></span>
                  <span>{ h.value }</span>
                </li>
              }) }
              </ul>
            </div>
            <div>
              <h2>Response-Headers</h2>
              <ul>
              { headers.map(h => {
                <li>
                  <span><strong>{ h.name }</strong></span>
                  <span>{ h.value }</span>
                </li>
              }) }
              </ul>
            </div>
            <div>
              <h2>TimeStamp</h2>
              <span>{ System.currentTimeMillis }</span>
            </div>
            </body>
          </html>
          
          response.addBody("""<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">""")
          response.addBody( res.toString )
          //response.addBody("<h1>HELLO WORLD</h1>")
          
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