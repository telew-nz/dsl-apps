package scala

import com.orientechnologies.orient.core.db._

import java.util.TimeZone
import java.nio.file.{Paths, Files, StandardOpenOption}
import scala.collection.mutable
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.db.record.OTrackedList

object ImportAllApps extends App {

    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    val dslDir = Paths.get("src/main/erp");

    // TODO: figure out how to automatically include .env values using Metals
    private val dsldb      = "dsldb"
    private val dbUrl      = "remote:localhost:2424" //sys.env("TLAYEN_DB_URL")
    private val rootDbUser = "root" //sys.env("TLAYEN_DB_ROOT_USER")
    private val dslDbUser  = "admin" //sys.env("TLAYEN_DSLDB_USER")
    private val password   = "Jkkat3puE4Qon251J@i#3T6Be" //sys.env("TLAYEN_DB_PASSWORD")

    implicit private val odb: OrientDB = new OrientDB(dbUrl, rootDbUser, password, OrientDBConfig.defaultConfig())
    private val session: ODatabaseSession = odb.open(dsldb, dslDbUser, password)
    session.activateOnCurrentThread()
    session.query("select from App").stream().forEach { resIter =>
        val app: ODocument = session.load[ODocument](resIter.getIdentity().get())
        val id = app.field[ORecordId]("@rid")
        val dslPackage = app.field[String]("dslPackage")
        println(s"App: id = $id, dslPackage = $dslPackage")

        val dir = dslDir.resolve(dslPackage)
        Files.createDirectories(dir)

        val manifest = dir.resolve("App.properties")
        val fields = app.toMap();
        fields.keySet.forEach { field =>
            if (!field.equals("files") && !field.equals("@class")) {
                val prop = s"$field = ${fields.get(field)}\n"
                Files.write(manifest, prop.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
            }
        }
    }

    session.query("select from AppSource").stream().forEach { resIter =>
        val source: ODocument = session.load[ODocument](resIter.getIdentity().get())
        val id = source.field[ORecordId]("@rid")
        val dslPackage = source.field[String]("dslPackage")
        val version = source.field[String]("version")
        val status = source.field[String]("status")
        println(s"AppSource: id = $id, dslPackage = $dslPackage, version = $version, status = $status")

        val dir = dslDir.resolve(dslPackage).resolve(version)
        Files.createDirectories(dir)

        val manifest = dir.resolve("AppSource.properties")
        val fields = source.toMap();
        fields.keySet.forEach { field =>
            if (!field.equals("files") && !field.equals("@class")) {
                val prop = s"$field = ${fields.get(field)}\n"
                Files.write(manifest, prop.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
            }
        }

        val files = source.field[OTrackedList[ODocument]]("files")
        files.forEach { file =>
            val name = file.field[String]("name")
            val content = file.field[String]("content")
            val f = dir.resolve(s"$name.erp")
            Files.write(f, content.getBytes())
        }
    }

    session.close()

}
