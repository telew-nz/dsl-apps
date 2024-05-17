package scala

import scala.jdk.CollectionConverters._

import com.orientechnologies.orient.core.db._

import java.util.TimeZone
import java.nio.file.{Paths, Files, StandardOpenOption}
import scala.collection.mutable
import com.orientechnologies.orient.core.record.impl.ODocument
import com.orientechnologies.orient.core.id.ORecordId
import com.orientechnologies.orient.core.db.record.OTrackedList

object ImportApps {

    def apply() = {

        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        val dslDir = Paths.get("src/main/erp");

        val dsldb      = "dsldb"
        val dbUrl           = sys.env("IMPORT_DB_URL")
        val rootDbUser      = sys.env("IMPORT_DB_ROOT_USER")
        val dslDbUser       = sys.env("IMPORT_DSL_DB_USER")
        val password        = sys.env("IMPORT_DB_PASSWORD")

        val odb: OrientDB = new OrientDB(dbUrl, rootDbUser, password, OrientDBConfig.defaultConfig())
        val session: ODatabaseSession = odb.open(dsldb, dslDbUser, password)
        session.activateOnCurrentThread()
        session.query("select from App").stream().forEach { resIter =>
            val app: ODocument = session.load[ODocument](resIter.getIdentity().get())
            val dslPackage = app.field[String]("dslPackage")
            if (dslPackage != "core" && !dslPackage.matches("^w\\d+_\\d+")) {
                val id = app.field[ORecordId]("@rid")
                println(s"App: id = $id, dslPackage = $dslPackage")
        
                val dir = dslDir.resolve(dslPackage)
                Files.createDirectories(dir)
        
                val manifest = dir.resolve("App.properties")
                val fields = app.toMap();
                val props = fields.keySet.stream.map { field =>
                    if (!List("files", "@class", "@rid", "versions").exists(field.equals(_)))
                        Option(s"$field = ${fields.get(field)}\n")
                    else
                        None
                }.toList().asScala.toSeq.flatten
                Files.write(manifest, props.mkString("").getBytes())
            }
        }

        session.query("select from AppSource").stream().forEach { resIter =>
            val source: ODocument = session.load[ODocument](resIter.getIdentity().get())
            val dslPackage = source.field[String]("dslPackage")
            if (dslPackage != "core" && !dslPackage.matches("^w\\d+_\\d+")) {
                val id = source.field[ORecordId]("@rid")
                val version = source.field[String]("version")
                val status = source.field[String]("status")
                println(s"AppSource: id = $id, dslPackage = $dslPackage, version = $version, status = $status")
        
                val dir = dslDir.resolve(dslPackage).resolve(version)
                Files.createDirectories(dir)
        
                val manifest = dir.resolve("AppSource.properties")
                val fields = source.toMap();
                val props = fields.keySet.stream.map { field =>
                    if (!List("files", "@class", "@rid", "app").exists(field.equals(_)))
                        Option(s"$field = ${fields.get(field)}\n")
                    else
                        None
                }.toList().asScala.toSeq.flatten
                Files.write(manifest, props.mkString("").getBytes())
        
                val files = source.field[OTrackedList[ODocument]]("files")
                files.forEach { file =>
                    val name = file.field[String]("name")
                    val content = file.field[String]("content")
                    val f = dir.resolve(s"$name.erp")
                    Files.write(f, content.getBytes())
                }
            }
        }

        session.close()

    }

}
