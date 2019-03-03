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
import dotterweide.Util.Version

trait DownloadVia {
  def run(scalaVersion           : Version = Version(2,12,8),
          maxScalaColliderVersion: Version = Version(1,28,0)): Option[File]
}
