package dotterweide

import java.io.File

import scala.util.Try

object CacheDir {
  def obtain(): Try[File] = Try {
    import net.harawata.appdirs.AppDirsFactory
    val appDirs = AppDirsFactory.getInstance
    val path    = appDirs.getUserCacheDir("dotterweide", /* version */ null, /* author */ null)
//    println(s"Cache path = $path")
    new File(path)
  }
}
