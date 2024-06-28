package com.dap

import com.orientechnologies.orient.core.db._
import com.orientechnologies.orient.core.db.record.OTrackedList
import com.orientechnologies.orient.core.record.impl.ODocument

import java.nio.file.{Files, Paths}
import java.text.SimpleDateFormat
import java.util.TimeZone
import scala.jdk.CollectionConverters._

object ExportApps {

    def apply(): Unit = {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        val dslDir = Paths.get("src/main/erp")

        val appPropertiesFilename = "App.properties"
        val appSourcePropertiesFilename = "AppSource.properties"

        val dsldb           = "dsldb"
        val dbUrl           = AppConfig.config.db.url
        val rootDbUser      = AppConfig.config.db.rootUser
        val dslDbUser       = AppConfig.config.db.dbUser
        val password        = AppConfig.config.db.password
        val overrideOwner   = AppConfig.config.exportDb.ownerOverride
        val overrideManager = AppConfig.config.exportDb.managerOverride

        val odb: OrientDB = new OrientDB(dbUrl, rootDbUser, password, OrientDBConfig.defaultConfig())
        val session: ODatabaseSession = odb.open(dsldb, dslDbUser, password)
        session.activateOnCurrentThread()

        session.command("""delete from App where not(dslPackage matches "^w\\d+_\\d+" or dslPackage = "core")""")
        session.command("""delete from AppSource where not(dslPackage matches "^w\\d+_\\d+" or dslPackage = "core")""")

        Files.list(dslDir).forEach { appDir =>
            println(s"~~> appDir = $appDir")

            val appDoc = new ODocument("App")
            val appProps = Files.readAllLines(appDir.resolve(appPropertiesFilename))
                .stream.map(_.split(" = "))
                .toList.asScala
                .filter(p => p.length == 2 && p.head != "@rid" && p.head != "versions")
                .flatten
                .grouped(2)
                .collect(x => x.head -> x.tail.head)
                .map {
                    case (k, v) if k == "owner" => k -> overrideOwner.getOrElse(v)
                    case (k, v) if k == "mng" => k -> overrideManager.getOrElse(v)
                    case any => any
                }
                .toMap
            appProps.foreach { case (fieldName, fieldValue) =>
                appDoc.field(fieldName, fieldValue)
            }
            appDoc.field("versions", "")
            appDoc.save()
            val appORID = appDoc.getIdentity
            Files.list(appDir).filter(_.getFileName.toString != appPropertiesFilename).toList.asScala.map { appSourceDir =>
                println(s"~~> appSourceDir = $appSourceDir")
                val appSourceDoc = new ODocument("AppSource")
                appSourceDoc.field("app", appORID)
                appSourceDoc.field("dependencies", "")
                Files.readAllLines(appSourceDir.resolve(appSourcePropertiesFilename))
                    .stream.map(_.split(" = "))
                    .toList.asScala
                    .filter(p => p.length == 2 && p.head != "app" && p.head != "@rid")
                    .flatten
                    .grouped(2)
                    .collect(x => x.head -> x.tail.head)
                    .foreach {
                        case (k, v) if k == "touchedOn" =>
                            val inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy")
                            val outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            appSourceDoc.field(k, outputFormat.format(inputFormat.parse(v)))
                        case (k, v) if k == "dependencies" =>
                            if (v != "[]")
                                v.drop(1).dropRight(1)
                                    .split("\\}, \\{")
                                    .map(_.dropWhile(_ == '{'))
                                    .map(_.reverse.dropWhile(_ == '}').reverse)
                                    .map(_.split(",(?=[a-z])").flatMap(prop => prop.split(":").padTo(2, "")).grouped(2).collect(x => x.head -> x.tail.head).toMap)
                                    .foreach(dep => {
                                        val dslPackage = dep.getOrElse("dslPackage", "")
                                        val version = dep.getOrElse("version", "")
                                        val channel = dep.getOrElse("channel", "")
                                        val depsDoc = new ODocument("dslPackage", dslPackage)
                                        depsDoc.field("version", version)
                                        depsDoc.field("channel", channel)
                                        appSourceDoc.field[OTrackedList[ODocument]]("dependencies").add(depsDoc)
                                    })
                        case (k, v) => appSourceDoc.field(k, v)
                    }
                appSourceDoc.field("files", "")
                Files.list(appSourceDir).filter(_.getFileName.toString != appSourcePropertiesFilename).forEach { appFile =>
                    val name = appFile.getFileName.toString.dropRight(4)
                    val content = Files.readAllLines(appFile).asScala.mkString("\n")
                    val fileDoc = new ODocument("name", name)
                    fileDoc.field("content", content)
                    appSourceDoc.field[OTrackedList[ODocument]]("files").add(fileDoc)
                }
                appSourceDoc.save()
                
                appDoc.field[OTrackedList[ODocument]]("versions").add(appSourceDoc)
                appDoc.save()
            }
        }

        session.close()

    }

}
