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

    private val dsldb           = "dsldb"
    // TODO: figure out how to automatically include .env values using Metals
    private val dbUrl           = "remote:localhost:2424" //sys.env("TLAYEN_DB_URL")
    private val rootDbUser      = "root" //sys.env("TLAYEN_DB_ROOT_USER")
    private val dslDbUser       = "admin" //sys.env("TLAYEN_DSLDB_USER")
    private val password        = "password" //sys.env("TLAYEN_DB_PASSWORD")
    private val overrideOwner   = Option("41_0") // sys.env.get("OVERRIDE_OWNER")
    private val overrideManager = Option("41_0") // sys.env.get("OVERRIDE_MANAGER")

    implicit private val odb: OrientDB = new OrientDB(dbUrl, rootDbUser, password, OrientDBConfig.defaultConfig())
    private val session: ODatabaseSession = odb.open(dsldb, dslDbUser, password)
    session.activateOnCurrentThread()

    Files.list(dslDir)
        .filter(d => d.getFileName().toString != "flow2b.integ.woocommerce" && d.getFileName().toString != "flow2b.farmlands" && d.getFileName().toString != "core" && !d.getFileName().toString.matches("^w\\d+_\\d+"))
        .forEach { appDir =>
        println(s"~~> appDir = $appDir")

        val appDoc = new ODocument("App")
        val appProps = Files.readAllLines(appDir.resolve(appPropertiesFilename))
            .stream.map(_.split(" = "))
            .toList().asScala
            .filter(p => p.length == 2 && p.head != "@rid" && p.head != "versions")
            .flatten
            .grouped(2)
            .collect(x => x.head -> x.tail.head)
            .map {
                case (k, v) if k == "owner" => k -> overrideOwner.getOrElse(v)
                case (k, v) if k == "mng" => k -> overrideOwner.getOrElse(v)
                case any => any
            }
            .toMap
        appProps.foreach { case (fieldName, fieldValue) =>
            appDoc.field(fieldName, fieldValue)
        }
        appDoc.field("versions", "")
        appDoc.save()
        val appORID = appDoc.getIdentity()
        Files.list(appDir).filter(_.getFileName().toString() != appPropertiesFilename).toList().asScala.map { appSourceDir =>
            println(s"~~> appSourceDir = $appSourceDir")
            val appSourceDoc = new ODocument("AppSource")
            appSourceDoc.field("app", appORID)
            appSourceDoc.field("dependencies", "")
            Files.readAllLines(appSourceDir.resolve(appSourcePropertiesFilename))
                .stream.map(_.split(" = "))
                .toList().asScala
                .filter(p => p.length == 2 && p.head != "app" && p.head != "@rid")
                .flatten
                .grouped(2)
                .collect(x => x.head -> x.tail.head)
                .foreach {
                    case (k, v) if k == "touchedOn" =>
                        val inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");
                        val outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        appSourceDoc.field(k, outputFormat.format(inputFormat.parse(v)))
                    case (k, v) if k == "dependencies" && v != "[]" =>
                        val depProps = v.drop(1).dropRight(1)
                            .split("\\}, \\{")
                            .map(_.dropWhile(_ == '{'))
                            .map(_.reverse.dropWhile(_ == '}').reverse)
                            .map(_.split(",(?=[a-z])").flatMap(prop => prop.split(":").padTo(2, "")).grouped(2).collect(x => x.head -> x.tail.head).toMap)
                            .foreach(dep => {
                                val depsDoc = new ODocument("dslPackage", dep.get("dslPackage").getOrElse(""))
                                depsDoc.field("version", dep.get("version").getOrElse(""))
                                depsDoc.field("channel", dep.get("channel").getOrElse(""))
                                appSourceDoc.field[OTrackedList[ODocument]]("dependencies").add(depsDoc)
                            })
                    case (k, v) => appSourceDoc.field(k, v)
                }
            appSourceDoc.field("files", "")
            Files.list(appSourceDir).filter(_.getFileName().toString() != appSourcePropertiesFilename).forEach { appFile =>
                val name = appFile.getFileName().toString().dropRight(4)
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
