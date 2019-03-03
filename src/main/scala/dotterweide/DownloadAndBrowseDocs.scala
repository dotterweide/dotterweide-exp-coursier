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

import de.sciss.file._
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.text.FontSmoothingType
import javafx.scene.web.WebView

import scala.swing.{Component, Dimension, MainFrame, Swing}

/** A simple test that downloads the unified scala-docs of ScalaCollider,
  * caching them in the `dotterweide` cache directory
  * (e.g. `~/.cache/dotterweide`), then unpacking the jar there,
  * then opening the index of `de.sciss.synth` in the browser.
  */
object DownloadAndBrowseDocs {
  case class Config(useCoursier: Boolean = false, useBrowser: Boolean = false, useDarkScheme: Boolean = true,
                    wipeCache: Boolean = false)

  def main(args: Array[String]): Unit = {
    val default = Config()

    val p = new scopt.OptionParser[Config]("Demo") {
      opt[Unit]("coursier")
        .text("Use Coursier instead of Dispatch")
        .action { (_, c) => c.copy(useCoursier = true) }

      opt[Unit]("browser")
        .text("Use web browser instead of JavaFX component")
        .action { (_, c) => c.copy(useBrowser = true) }

      opt[Unit]("light")
        .text("Use light colour scheme instead of dark")
        .action { (_, c) => c.copy(useDarkScheme = false) }

      opt[Unit]("wipe-cache")
        .text("Wipe cached javadoc files")
        .action { (_, c) => c.copy(wipeCache = true) }
    }
    p.parse(args, default).fold(sys.exit(1)) { config =>
      run(config)
    }
  }

  def run(config: Config): Unit = {
    import config._

    val target    = unpackDir
    val styleDir  = target / "lib"
    val index     = target / "de" / "sciss" / "synth" / "index.html"

    def proceed(): Unit =
      setStyleAndOpenDocs(styleDir = styleDir, index = index, useBrowser = useBrowser, useDarkScheme =  useDarkScheme)

    if (index.isFile && wipeCache) {
      println("Wiping cache...")
      Util.deleteRecursive(target)
    }

    if (index.isFile) {
      println("Reusing cached docs.")
      proceed()
    } else {
      val downloader = if (useCoursier) DownloadViaCoursier else DownloadViaDispatch
      downloader.run().foreach { jar =>
        unpackDocs(jar, target = target)
        proceed()
      }
    }
  }

  def copyResource(name: String, out: File): Unit = {
    val is = new BufferedInputStream(getClass.getResourceAsStream(name))
    try {
      val os = new BufferedOutputStream(new FileOutputStream(out))
      try {
        var byte = 0
        while ({ byte = is.read(); byte != -1 }) {
          os.write(byte)
        }
      } finally  {
        os.close()
      }
    } finally {
      is.close()
    }
  }

  def setStyleAndOpenDocs(styleDir: File, index: File, useBrowser: Boolean, useDarkScheme: Boolean): Unit = {
    val tpe = if (useDarkScheme) "dark" else "light"
    copyResource(s"index-$tpe.css"    , styleDir / "index.css")
    copyResource(s"template-$tpe.css" , styleDir / "template.css")
    openDocs(index, useBrowser = useBrowser)
  }

  def openDocs(index: File, useBrowser: Boolean): Unit =
    if (useBrowser) {
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
        val web = new WebView
        web.setFontSmoothingType(FontSmoothingType.GRAY)
        // web.setContextMenuEnabled(true)
        // web.setFontScale(1.2)
        // web.setZoom(2.0)
        val scene = new Scene(web)
        web.getEngine.load(index.toURI.toString)
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
}
