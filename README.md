# Tlayen Apps

This contains a copy of all the source code for all the Tlayen apps.

## Updating

The source code can be updated using [ImportAllApps] script.

## Latest

Having all versions at once makes it difficult to search and so you can use the [latest] branch to view only the latest
version of each app.

To delete all versions other than the latest run the following Scala script:

```scala worksheet
import java.io.FilenameFilter
import java.nio.file.attribute.BasicFileAttributes

import scala.math.Ordering

implicit val versionOrdering = new Ordering[String] {
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

import java.nio.file._
import java.io._

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

val erpDir = Paths.get("src/main/erp")
erpDir.toFile.listFiles().foreach { dir =>
  dir
    .list(versionFilter)
    .sorted(versionOrdering.reverse)
    .drop(1)
    .foreach(s => deleteRecursively(dir.toPath.resolve(s)))
}
```

[ImportAllApps]: ./src/main/scala/ImportAllApps.scala
[latest]: https://github.com/tlayen/apps/tree/latest/src/main/erp
