/*
 *  DownloadAndBrowseDocs.scala
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
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView

import scala.concurrent.ExecutionContext.Implicits.global
import scala.swing.{Component, Dimension, MainFrame, Swing}

/** A simple test that downloads the unified scala-docs of ScalaCollider,
  * caching them in the `dotterweide` cache directory
  * (e.g. `~/.cache/dotterweide`), then unpacking the jar there,
  * then opening the index of `de.sciss.synth` in the browser.
  */
object DownloadAndBrowseDocs {
  val USE_BROWSER = false   // if `false`, display in JavaFX panel

  def main(args: Array[String]): Unit = {
    val target  = unpackDir
    val index   = target / "de" / "sciss" / "synth" / "index.html"
    if (index.isFile) {
      openDocs(index)
    } else {
      downloadDocs().foreach { jar =>
        unpackDocs(jar, target = target)
        openDocs(index)
      }
    }
  }

  def openDocs(index: File): Unit =
    if (USE_BROWSER) {
      println("Opening web browser...")
      Desktop.getDesktop.browse(index.toURI)
    } else Swing.onEDT {
      println("Opening JavaFX view...")
      // cf. https://docs.oracle.com/javase/8/javafx/interoperability-tutorial/swing-fx-interoperability.htm
      val fxPanel = new JFXPanel
      new MainFrame {
        title     = "API Browser"
        contents  = Component.wrap(fxPanel)
        size      = new Dimension(960, 720)
        centerOnScreen()
        open()
      }
      javafx.application.Platform.runLater(Swing.Runnable {
        val web   = new WebView
        web.getEngine.load(index.toURI.toString)
        // web.setZoom(2.0)
        val scene = new Scene(web)
        fxPanel.setScene(scene)
      })
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
        val f = target / entry.getName
        if (entry.isDirectory) {
          f.mkdirs()
        } else {
          // cf. http://stackoverflow.com/questions/8909743/jarentry-getsize-is-returning-1-when-the-jar-files-is-opened-as-inputstream-f
          // TODO: this is very slow, is there a faster way?
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

  def unpackDir: File = CacheDir.obtain().toOption.fold(File.createTemp(directory = true))(_ / "doc")

  def unpackDocs(jar: File, target: File): Unit = {
    println(s"Unpacking in $target...")
    unpackJar(jar, target)
    println("Done.")
  }

  def downloadDocs(): Option[File] = {
    val mod = Module(org"de.sciss", name"scalacollider-unidoc_2.12")

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
