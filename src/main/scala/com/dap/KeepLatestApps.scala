package com.dap

import java.io.{File, FilenameFilter, IOException}
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file._
import scala.annotation.{tailrec, unused}


@unused
object KeepLatestApps {

    private val dslDir = Paths.get("src/main/erp")

    implicit val versionOrdering: Ordering[String] = new Ordering[String] {
        override def compare(x: String, y: String): Int = {
            compareParts(toVersion(x), toVersion(y))
        }

        private def toVersion(a: String): List[Int] = a.split('.').map(_.toInt).toList

        @tailrec
        private def compareParts(a: List[Int], b: List[Int]): Int = {
            a match {
            case Nil => b match {
                case Nil => 0
                case 0 :: bTail => compareParts(a, bTail)
                case _ => -1
            }
            case aHead :: aTail => b match {
                case Nil => aHead
                case `aHead` :: bTail => compareParts(aTail, bTail)
                case bHead :: _ => aHead - bHead
            }
            }
        }
    }


    private val versionFilter: FilenameFilter = (_: File, name: String) => name.matches("""\d+(\.\d+)*""")

    private def deleteRecursively(path: Path): Path = {
    Files.walkFileTree(path, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
            Files.delete(file)
            FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult = {
            Files.delete(dir)
            FileVisitResult.CONTINUE
        }
    })
    }

    dslDir.toFile.listFiles().foreach { dir =>
        dir
            .list(versionFilter)
            .sorted(versionOrdering.reverse)
            .drop(1)
            .foreach(s => deleteRecursively(dir.toPath.resolve(s)))
    }

}
