package scala

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.javadsl.model

import scala.io.StdIn

object DslSyncServer {
  
    def main(args: Array[String]): Unit = {

        implicit val system = ActorSystem(Behaviors.empty, "my-system")
        implicit val executionContext = system.executionContext

        val route =
            path("export-db-dsl") {
                get {
                    parameter('dslPackage.as[String]) { (dslPackage) =>
                        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, s"<h1>Testing $dslPackage</h1>"))
                    }
                }
            }
        
        val port = sys.env("DSL_SYNC_SERVER_PORT").toInt
        val bindingFuture = Http().newServerAt("localhost", port).bind(route)

        println(s"Server now online (port=$port).\nPress RETURN to stop...")
        StdIn.readLine()
        bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ => system.terminate())

    }

}
