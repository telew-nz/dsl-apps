import java.io.{File, FilenameFilter, IOException}
import java.nio.file.{Files, FileVisitResult, Path, Paths, SimpleFileVisitor}
import java.nio.file.attribute.BasicFileAttributes

import scala.math.Ordering


object DeleteAllButLatest extends App {

    private val dslDir = Paths.get("src/main/erp");

    implicit val versionOrdering: Ordering[String] = new Ordering[String] {
        override def compare(x: String, y: String) = {
            compareParts(toVersion(x), toVersion(y))
        }

        private def toVersion(a: String): List[Int] = a.split('.').map(_.toInt).toList

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


    val versionFilter = new FilenameFilter {
        override def accept(dir: File, name: String) = name.matches("""\d+(\.\d+)*""")
    }

    def deleteRecursively(path: Path) = {
    Files.walkFileTree(path, new SimpleFileVisitor[Path] {
        override def visitFile(file: Path, attrs: BasicFileAttributes) = {
            Files.delete(file)
            FileVisitResult.CONTINUE
        }

        override def postVisitDirectory(dir: Path, exc: IOException) = {
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
