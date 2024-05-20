package scala

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.javadsl.model

import scala.io.StdIn
import scala.util.{Try, Success, Failure}

object DslSyncServer {
  
    def main(args: Array[String]): Unit = {

        implicit val system = ActorSystem(Behaviors.empty, "my-system")
        implicit val executionContext = system.executionContext

        val routes =
            path("export-db-dsl") {
                post {
                    Try {
                        println(s"Pushing changes")
                        ImportApps()
                        GithubIntegration.pushChanges
                    } match {
                        case Success(_) => complete(StatusCodes.Created)
                        case Failure(err) =>
                            println(s"[ERR] ${err.getMessage()}")
                            complete(StatusCodes.InternalServerError)
                    }
                }
            } ~ path("import-db-dsl") {
                post {
                    Try {
                        println(s"Pulling from repo")
                        GithubIntegration.pullChanges
                        ExportApps()
                    } match {
                        case Success(_) => complete(StatusCodes.Created)
                        case Failure(err) =>
                            println(s"[ERR] ${err.getMessage()}")
                            complete(StatusCodes.InternalServerError)
                    }
                }
            }
        
        val address = sys.env("DSL_SYNC_SERVER_ADDRESS")
        val port = sys.env("DSL_SYNC_SERVER_PORT").toInt
        val bindingFuture = Http().newServerAt(address, port).bind(routes)

        println(s"Server now online ($address:$port).")
        
        sys.addShutdownHook {
            bindingFuture
                .flatMap(_.unbind())
                .onComplete(_ => {
                    system.terminate()
                })
        }

    }

}
