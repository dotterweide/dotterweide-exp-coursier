/*
 *  FindMostRecentVersion.scala
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
import coursier._
import coursier.core.Classifier
import coursier.util.Task

import scala.concurrent.ExecutionContext.Implicits.global

object FindMostRecentVersion {
  def main(args: Array[String]): Unit = {
    findDocs()
  }

  def findDocs(): Unit = {
    val mod = Module(org"de.sciss", name"scalacollider_2.12")

    val start = Resolution(
      Set(
        Dependency(mod, version = "") // doesn't work
      )
    )

    val repositories = Seq(
//      Cache.ivy2Local,
      MavenRepository("https://repo1.maven.org/maven2")
    )

    val fetch: Metadata[Task] = Fetch.from(repositories, Cache.fetch[Task]())

    println("Resolving...")
    val res: Resolution = start.process.run(fetch).unsafeRun()
    println("Done.")

    val errors: Seq[((Module, String), Seq[String])] = res.errors

    if (errors.nonEmpty) {
      println("There were errors:")
      errors.foreach { err =>
        println(err)
      }
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
  }
}
