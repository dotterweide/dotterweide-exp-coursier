/*
 *  DownloadViaCoursier.scala
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

import coursier.Fetch.Metadata
import coursier.core.Classifier
import coursier.util.Task
import coursier.{Artifact, Attributes, Cache, Dependency, Fetch, FileError, MavenRepository, Resolution, Module => CModule, _}
import de.sciss.file._
import dotterweide.Util.Module

import scala.concurrent.ExecutionContext.Implicits.global

object DownloadViaCoursier extends DownloadVia {
  def run(scalaVersion: Util.Version, module0: Module): Option[File] = {
    val module = CModule(Organization(module0.groupId), ModuleName(module0.artifactId))

    val start = Resolution(
      Set(
        Dependency(module, version = module0.version.toString)
      )
    )

    val repositories = Seq(
      Cache.ivy2Local,
      MavenRepository(Util.mavenCentralBase)
    )

    val fetch: Metadata[Task] = Fetch.from(repositories, Cache.fetch[Task]())

    println("Resolving...")
    val res: Resolution = start.process.run(fetch).unsafeRun()
    println("Done.")

    val errors: Seq[((CModule, String), Seq[String])] = res.errors

    if (errors.nonEmpty) {
      println("There were errors:")
      errors.foreach(println)
    }

    val a0: Set[(Dependency, Attributes, Artifact)] = res.dependencyArtifacts(
      classifiers = Some(Seq(Classifier.javadoc))
    ).toSet // there are redundancies

    val a = a0.filter(_._1.module == module)
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
