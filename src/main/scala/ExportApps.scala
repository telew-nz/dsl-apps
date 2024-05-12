package scala

import scala.jdk.CollectionConverters._

import com.orientechnologies.orient.core.db._

import java.util.TimeZone
import java.nio.file.{Paths, Files, StandardOpenOption}
import scala.collection.mutable
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.db.record.OTrackedList
import java.text.SimpleDateFormat
import com.orientechnologies.orient.core.metadata.schema.OType

object ExportApps extends App {

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    val dslDir = Paths.get("src/main/erp");

    private val appPropertiesFilename = "App.properties"
    private val appSourcePropertiesFilename = "AppSource.properties"

    // TODO: figure out how to automatically include .env values using Metals
    private val dsldb      = "dsldb"
    private val dbUrl      = "remote:localhost:2424" //sys.env("TLAYEN_DB_URL")
    private val rootDbUser = "root" //sys.env("TLAYEN_DB_ROOT_USER")
    private val dslDbUser  = "admin" //sys.env("TLAYEN_DSLDB_USER")
    private val password   = "password" //sys.env("TLAYEN_DB_PASSWORD")

    implicit private val odb: OrientDB = new OrientDB(dbUrl, rootDbUser, password, OrientDBConfig.defaultConfig())
    private val session: ODatabaseSession = odb.open(dsldb, dslDbUser, password)
    session.activateOnCurrentThread()

    Files.list(dslDir).filter(_.getFileName().toString != "core").forEach { appDir =>
        println(s"~~> appDir = $appDir")

        val appDoc = new ODocument("App")
        val appProps = Files.readAllLines(appDir.resolve(appPropertiesFilename))
            .stream.map(_.split(" = "))
            .toList().asScala
            .filter(p => p.length == 2 && p.head != "@rid" && p.head != "versions")
            .flatten
            .grouped(2)
            .collect(x => x.head -> x.tail.head)
            .toMap
        appProps.foreach { case (fieldName, fieldValue) =>
            appDoc.field(fieldName, fieldValue)
        }
        appDoc.field("versions", "")
        appDoc.save()
        val appORID = appDoc.getIdentity()
        Files.list(appDir).filter(_.getFileName().toString() != appPropertiesFilename).toList().asScala.map { appSourceDir =>
            println(s"~~> appSourceDir = $appSourceDir")
            val appSourceProps = Files.readAllLines(appSourceDir.resolve(appSourcePropertiesFilename))
                .stream.map(_.split(" = "))
                .toList().asScala
                .filter(p => p.length == 2 && p.head != "app" && p.head != "@rid")
                .flatten
                .grouped(2)
                .collect(x => x.head -> x.tail.head)
                .map {
                    case (k, v) if k == "touchedOn" =>
                        val inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                        val outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        k -> outputFormat.format(inputFormat.parse(v))
                    case (k, v) if k == "dependencies" =>
                        k -> v.drop(1).dropRight(1)
                    case any => any
                }
                .toMap
            val appSourceDoc = new ODocument("AppSource")
            appSourceProps.foreach { case (fieldName, fieldValue) =>
                appSourceDoc.field(fieldName, fieldValue)
            }
            appSourceDoc.field("app", appORID)
            val fileDocs = Files.list(appSourceDir).filter(_.getFileName().toString() != appSourcePropertiesFilename).map { appFile =>
                val name = appFile.getFileName().toString().dropRight(4)
                val content = Files.readAllLines(appFile).asScala.mkString("\n")
                s"{name:$name,content:$content}}"
            }.toList.asScala.toList
            appSourceDoc.field("files", fileDocs.mkString(","))
            appSourceDoc.save()
            appDoc.field[OTrackedList[ODocument]]("versions").add(appSourceDoc)
        }
        appDoc.save()
    }

    session.close()

}
