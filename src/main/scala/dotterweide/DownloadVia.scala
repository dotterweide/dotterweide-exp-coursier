/*
 *  DownloadVia.scala
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

import de.sciss.file.File
import dotterweide.Util.{Module, Version}

trait DownloadVia {
  def run(scalaVersion: Version = Version(2,12,8),
          latestModule: Module = Module("org.scala-lang", "scala-library_2.12", Version(2,12,8))): Option[File]
}
