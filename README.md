# dotterweide-exp-coursier

The dotterweide projects aims to develop an embeddable mini-IDE with support for the Scala programming language.
Please refer to the [organisational project](https://github.com/dotterweide/dotterweide-org) for further information.

This `-exp-coursier` repository contains experimental code to understand 
the [Coursier](https://get-coursier.io/) library, among other things, and how it can be used to access library
dependencies, their scaladocs and source code. This repository is covered by the
[GNU Lesser General Public License v2.1](https://www.gnu.org/licenses/lgpl-2.1.txt) or higher.

The project build with [sbt](http://www.scala-sbt.org/) with the main Scala version being 2.12.x.

See `src/main/scala/dotterweise/DownloadAndBrowseDocs.scala` for the main class. The following directories/files 
will be created and could be clean-up after the experiments:

 - `~/.cache/dotterweide` - where the scala-doc files are extracted. (location will be different on Mac and Windows)
 - `~/.coursier` - where Coursier is caching downloads (location will be different on Mac and Windows); only if
  using Coursier (`--coursier` switch to the main class)
  
We now test using [dispatch/reboot](https://github.com/dispatch/reboot) instead of Coursier, as the latter does not
offer any feature that would be useful here, including finding the most recent published version.

----

Basically you `sbt run` to test the doc browser. `sbt 'run --help'` to see all options.

__To-do:__ There is an issue with an NPE being thrown when closing the window,
I guess JavaFX needs a special disposal first.
