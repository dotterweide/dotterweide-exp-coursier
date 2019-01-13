/*
 *  CoursierTest.scala
 *  (Dotterweide)
 *
 *  Copyright (c) 2019 the Dotterweide authors. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package dotterweide

import java.awt.Desktop
import java.io.{BufferedInputStream, BufferedOutputStream, ByteArrayInputStream, FileInputStream, FileOutputStream}

import coursier.Fetch.Metadata
import coursier._
import coursier.core.Classifier
import coursier.util.Task
import de.sciss.file._

import scala.concurrent.ExecutionContext.Implicits.global

/** A simple test that downloads the scala-docs of ScalaCollider,
  * caching them in the `dotterweide` cache directory
  * (e.g. `~/.cache/dotterweide`), then unpacking the jar there,
  * then opening the index of `de.sciss.synth` in the browser.
  *
  * Currently we unpack always, even if the jar had been unpacked
  * before.
  *
  * To-dos:
  *
  * - on the website we use sbt-unidoc (https://github.com/sbt/sbt-unidoc),
  *   so we have all libraries interlinked. A solution could be to
  *   '''publish''' the javadoc of a unidoc project, and then download
  *   that instead?
  */
object CoursierTest {
  def main(args: Array[String]): Unit = {
    downloadDocs().foreach { jar =>
      val baseDir = unpackDocs(jar)
      val index   = baseDir / "de" / "sciss" / "synth" / "index.html"
      Desktop.getDesktop.browse(index.toURI)
    }
  }

  def unpackJar(jar: File, target: File): Seq[File] = {
    import java.util.jar._

    import scala.annotation.tailrec

    val jarIn = new BufferedInputStream(new FileInputStream(jar))
    val bytes = try {
      val jarSz = jarIn.available()
      val arr = new Array[Byte](jarSz)
      jarIn.read(arr)
      arr
    } finally {
      jarIn.close()
    }
    val in    = new JarInputStream(new ByteArrayInputStream(bytes))
    val b     = Seq.newBuilder[File]

    @tailrec def loop(): Unit = {
      val entry: JarEntry = in.getNextJarEntry
      if (entry != null) {
//        println(entry.getName)
        val f = target / entry.getName
        if (entry.isDirectory) {
          f.mkdirs()
        } else {
          // cf. http://stackoverflow.com/questions/8909743/jarentry-getsize-is-returning-1-when-the-jar-files-is-opened-as-inputstream-f
          val bs  = new BufferedOutputStream(new FileOutputStream(f))
          var i   = 0
          while (i >= 0) {
            i = in.read()
            if (i >= 0) bs.write(i)
          }
          bs.close()
        }
        b += f
        loop()
      }
    }
    loop()
    in.close()
    b.result()
  }

  def unpackDocs(jar: File): File = {
    val unpackDir = CacheDir.obtain().toOption.fold(File.createTemp(directory = true))(_ / "doc")
    println(s"Unpacking in $unpackDir...")
    unpackJar(jar, unpackDir)
    println("Done.")
    unpackDir // / "index.html"
  }

  def downloadDocs(): Option[File] = {
    val mod = Module(org"de.sciss", name"scalacollider_2.12")

    val start = Resolution(
      Set(
        Dependency(mod, version = "1.28.0")
      )
    )

    val repositories = Seq(
      Cache.ivy2Local,
      MavenRepository("https://repo1.maven.org/maven2")
    )

    val fetch: Metadata[Task] = Fetch.from(repositories, Cache.fetch[Task]())

    println("Resolving...")
    val res: Resolution = start.process.run(fetch).unsafeRun()
    println("Done.")

    val errors: Seq[((Module, String), Seq[String])] = res.errors

    if (errors.nonEmpty) {
      println("There were errors:")
      errors.foreach(println)
    }

    val a0: Set[(Dependency, Attributes, Artifact)] = res.dependencyArtifacts(
      classifiers = Some(Seq(Classifier.javadoc))
    ).toSet // there are redundancies

    val a = a0.filter(_._1.module == mod)
    require (a.size == 1)

    val (sourcesDep, sourcesAttr, sourcesArt) = a.head

    println(s"dependency: $sourcesDep")
    println(s"attributes: $sourcesAttr")
    println(s"artifact  : $sourcesArt")

    println("Downloading...")
//    println(s"default cache = ${Cache.default}")
    val cacheDir = CacheDir.obtain().toOption.fold(Cache.default)(_ / "coursier")
    val local: Either[FileError, File] = Cache.file[Task](sourcesArt, cache = cacheDir).run.unsafeRun()
    println("Done.")

    local match {
      case Right(f) =>
        println(s"local     : $f")
        Some(f)

      case Left(err) =>
        println("There was an error:")
        println(err)
        None
    }
  }
}
