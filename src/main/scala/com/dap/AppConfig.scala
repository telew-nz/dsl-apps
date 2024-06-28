package com.dap

import pureconfig._
import pureconfig.generic.auto._

object AppConfig {

    val config: DslSyncServerConfig = ConfigSource.default.load[RootConfig] match {
        case Right(value) => value.dslSyncServer
        case Left(err) => throw new RuntimeException(
            "Error loading config:" + err.toList.map(_.description).mkString("\n")
        )
    }

}

case class RootConfig(dslSyncServer: DslSyncServerConfig)
case class DslSyncServerConfig(host: String, port: Int, db: DbConfig, exportDb: ExportDbConfig, github: GithubConfig)
case class DbConfig(url: String, rootUser: String, dbUser: String, password: String)
case class ExportDbConfig(ownerOverride: Option[String], managerOverride: Option[String])
case class GithubConfig(sshPath: String, userName: String, userEmail: String)
