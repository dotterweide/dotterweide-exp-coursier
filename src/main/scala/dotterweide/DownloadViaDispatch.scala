/*
 *  DownloadViaDispatch.scala
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

import java.util.{concurrent => juc}

import de.sciss.file._
import de.sciss.model.impl.ModelImpl
import de.sciss.processor.Processor
import de.sciss.processor.impl.FutureProxy
import dispatch._
import dispatch.Defaults._
import dotterweide.Util.{Module, Version}
import org.asynchttpclient.{AsyncHandler, ListenableFuture, Request}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import scala.util.Try

object DownloadViaDispatch extends DownloadVia {
  implicit class ModuleOps(private val m: Module) extends AnyVal {
    import m._

    def mkBaseUrl(repoBase: Req): Req =
      repoBase / groupId.replace('.', '/') / artifactId

    def mkMetaDataUrl(repoBase: Req): Req =
      mkBaseUrl(repoBase) / "maven-metadata.xml"

    def mkJavaDocUrl(repoBase: Req): Req =
      mkBaseUrl(repoBase) / version.toString / s"$artifactId-$version-javadoc.jar"
  }

  def run(scalaVersion: Version, module: Module): Option[File] = {
    val repo                  = url("https://repo1.maven.org/maven2")
    val metaDataURL           = module.mkMetaDataUrl(repo)

    println("Resolving artifact...")
    val metaDataFut: Future[xml.Elem] = Http.default(metaDataURL OK as.xml.Elem)

    val metaData        = Await.result(metaDataFut, Duration.Inf)
    // println(metaData)

    val metaGroupId     = (metaData \ "groupId"    ).text.trim
    val metaArtifactId  = (metaData \ "artifactId" ).text.trim
    val metaVersioning  = metaData \ "versioning"
    val lastUpdated     = (metaVersioning \ "lastUpdated" ).text.trim.toLong
    val latestVersion   = Version.parse((metaVersioning \ "latest").text)
    val metaVersions    = (metaVersioning \ "versions" \ "version").flatMap(n => Version.parse(n.text).toOption).sorted.reverse
    val bestVersionOpt  = metaVersions.find(_ <= module.version) // don't care about bin-compat here

    println(s"Last updated: $lastUpdated; latest version: $latestVersion")
    println("All versions:")
    metaVersions.foreach(println)
    println(s"Best matching version: $bestVersionOpt")

    require (metaGroupId    == module.groupId   , s"$metaGroupId != ${module.groupId}")
    require (metaArtifactId == module.artifactId, s"$metaArtifactId != ${module.artifactId}")

    bestVersionOpt.map { bestVersion =>
      println("Downloading...")
      println("_" * 100)
      val moduleDl    = module.copy(version = bestVersion)
      val reqDl       = moduleDl.mkJavaDocUrl(repo)
      val fDownload   = File.createTemp(suffix = ".jar")
      val futDownload = runWith(req = reqDl, out = fDownload, info = "Download docs")
      var progLast    = 0

      futDownload.addListener {
        case pp @ Processor.Progress(_, _) =>
          val progNow = pp.toInt
          while (progLast < progNow) {
            print('#')
            progLast += 1
          }
      }
      Await.result(futDownload, Duration.Inf)
      println(" Done.")
      fDownload
    }
  }

  def runWith(req: dispatch.Req, out: File, info: String): Processor[Unit] = {
    import dispatch._
    import Defaults._

    if (out.isFile) out.delete()

    new Impl(info, out) {
      var progress: Double = 0.0

      private[this] val handler = FileWithProgress(out) { (pos, size) =>
        val p = pos.toDouble / size
        // println(s"progress: $p% ($pos of $size)")
        progress = p
        dispatch(Processor.Progress(this, p))
      }

      private[this] val reqH: (Request, AsyncHandler[_]) = req > handler
      private[this] val lFut: ListenableFuture[_] = Http.default.client.executeRequest(reqH._1, reqH._2) // XXX TODO --- this can block
      private[this] val pr    = Promise[Unit]()

      protected def peerFuture: Future[Unit] = pr.future

      lFut.addListener(
        new Runnable {
          def run(): Unit = pr.complete(Try[Unit](lFut.get()))
        },
        new juc.Executor {
          def execute(runnable: Runnable): Unit = executor.execute(runnable)
        }
      )

      def abort(): Unit = lFut.abort(Processor.Aborted())
    }
  }

  private abstract class Impl(info: String, out: File)
    extends Processor[Unit]
      with FutureProxy[Unit]
      with ModelImpl[Processor.Update[Unit, Processor[Unit]]] {

    override def toString = s"Download($info, $out) - ${peerFuture.value}"
  }
}
