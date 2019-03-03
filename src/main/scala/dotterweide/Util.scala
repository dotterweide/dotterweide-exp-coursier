package dotterweide

import java.nio.file.Path

import de.sciss.file.File

object Util {
  val mavenCentralBase = "https://repo1.maven.org/maven2"

  object Version {
    def parse(s: String): Version = {
      val s0 = s.trim.split('.')
      require (s0.length == 3)
      val Array(epoch, major, minor) = s0
      Version(epoch = epoch.toInt, major = major.toInt, minor = minor.toInt)
    }
  }
  case class Version(epoch: Int, major: Int, minor: Int) extends Ordered[Version] {
    def binCompat: String = s"$epoch.$major"

    override def toString = s"$epoch.$major.$minor"

    def compare(that: Version): Int = {
      if      (this.epoch < that.epoch) -1 else if (this.epoch > that.epoch) +1
      else if (this.major < that.major) -1 else if (this.major > that.major) +1
      else if (this.minor < that.minor) -1 else if (this.minor > that.minor) +1
      else 0
    }
  }

  /** Deletes a directory and its contents recursively, _not_ following symlinks. */
  def deleteRecursive(dir: File): Unit = {
    // https://stackoverflow.com/questions/779519/delete-directories-recursively-in-java/27917071#27917071
    import java.io.IOException
    import java.nio.file.{FileVisitResult, Files, SimpleFileVisitor}
    import java.nio.file.attribute.BasicFileAttributes

    Files.walkFileTree(dir.toPath, new SimpleFileVisitor[Path]() {
      private def delete(p: Path): FileVisitResult = {
        Files.delete(p)
        // println(s"DELETING $p")
        FileVisitResult.CONTINUE
      }

      override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult =
        delete(file)

      override def postVisitDirectory(dir: Path, exc: IOException): FileVisitResult =
        delete(dir)
    })
  }
}
