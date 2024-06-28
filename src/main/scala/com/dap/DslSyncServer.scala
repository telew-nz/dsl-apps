package com.dap

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

object DslSyncServer {

    def main(args: Array[String]): Unit = {

        implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "my-system")
        implicit val executionContext: ExecutionContextExecutor = system.executionContext

        GithubIntegration.prepareGitUser()

        val routes =
            path("export-db-dsl") {
                post {
                    Try {
                        println(s"Pushing changes")
                        ImportApps()
                        GithubIntegration.pushChanges()
                    } match {
                        case Success(_) => complete(StatusCodes.Created)
                        case Failure(err) =>
                            println(s"[ERR] ${err.getMessage}")
                            complete(StatusCodes.InternalServerError)
                    }
                }
            } ~ path("import-db-dsl") {
                post {
                    Try {
                        println(s"Pulling from repo")
                        GithubIntegration.pullChanges()
                        ExportApps()
                    } match {
                        case Success(_) => complete(StatusCodes.Created)
                        case Failure(err) =>
                            println(s"[ERR] ${err.getMessage}")
                            complete(StatusCodes.InternalServerError)
                    }
                }
            }

        val address = AppConfig.config.host
        val port = AppConfig.config.port
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
