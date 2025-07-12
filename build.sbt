scalaVersion := "3.3.6" // A Long Term Support version.

enablePlugins(ScalaNativePlugin)

// set to Debug for compilation details (Info is default)
logLevel := Level.Info

// import to add Scala Native options
import scala.scalanative.build._

// defaults set with common options shown
nativeConfig ~= { c =>
  c.withLTO(LTO.none) // thin
    .withMode(Mode.debug) // releaseFast
    .withGC(GC.immix) // commix
}

libraryDependencies += "org.scala-lang.modules" %%% "scala-parser-combinators" % "2.4.0"
libraryDependencies += "com.lihaoyi" %%% "os-lib" % "0.11.4"
libraryDependencies += "com.monovore" %%% "decline" % "2.5.0"
libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.19" % Test
