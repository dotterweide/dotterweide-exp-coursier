# dotterweide-exp-coursier

The dotterweide projects aims to develop an embeddable mini-IDE with support for the Scala programming language. Please refer to the
[organisational project](https://github.com/dotterweide/dotterweide-org) for further information.

This `-exp-coursier` repository contains experimental code to understand the [Coursier](https://get-coursier.io/) library, and
how it can be used to access library dependencies, their scaladocs and source code.
This repository is covered by the [GNU Lesser General Public License v2.1](https://www.gnu.org/licenses/lgpl-2.1.txt) or higher.

The project build with [sbt](http://www.scala-sbt.org/) with the main Scala version being 2.12.x.

See `src/main/scala/dotterweise/DownloadAndBrowseDocs.scala` for the main class. The following directories/files will be created and
could be clean-up after the experiments:

 - `~/.cache/dotterweide` - where the scala-doc files are extracted. (location will be different on Mac and Windows)
 - `~/.coursier` - where Coursier is caching downloads (location will be different on Mac and Windows)

Note: On Linux, JavaFX has really horrible font rendering with off-colour sub-pixels. To have less noisy display, add
`-Dprism.lcdtext=false` when running.