package dev.capslock.allyas

import scala.scalanative.unsafe.Zone
import scalanative.runtime.filename
import scala.util.Success
import scala.util.Failure
import scala.util.parsing.combinator.Parsers
import com.monovore.decline.*
import cats.implicits.*

inline def eprintln(msg: String): Unit = Console.err.println(msg)

case class Config(
    configFile: Option[String],
    verbose: Boolean = false
)

object Main {
  val verboseFlag = Opts.flag("verbose", "Enable verbose output", "v").orFalse

  val command = Command(
    name = "allyas",
    header = "A command aliasing tool"
  )(verboseFlag)

  def main(args: Array[String]): Unit = {
    if (args.length > 0 && args(0) == "shim") {
      val exitCode = runShim()
      sys.exit(exitCode)
    } else {
      command.parse(args.toList) match {
        case Left(help) =>
          eprintln(help.toString)
          sys.exit(1)
        case Right(verbose) =>
          val config = Config(Environments.ALLYAS_CONF, verbose)
          val exitCode = run(config)
          sys.exit(exitCode)
      }
    }
  }

  def run(config: Config): Int = {
    val confFile = config.configFile match {
      case Some(file) => file
      case None =>
        eprintln("*** Allyas: Please set the ALLYAS_CONF environment variable.")
        return 1
    }

    if (config.verbose) {
      eprintln(s"*** Allyas: Using config file: $confFile")
    }

    val confString =
      try {
        os.read(os.Path(confFile, os.pwd))
      } catch {
        case e: Exception =>
          eprintln(s"*** Allyas: Error reading config file: ${e.getMessage}")
          return 1
      }

    val parser = SExpressionParser()
    val conf = parser.parse(parser.config, confString)

    if (!conf.successful) {
      eprintln(s"*** Allyas: Error parsing config file: ${conf.toString}")
      return 1
    }

    val whoami = if (filename.startsWith("/")) {
      os.Path(filename).baseName
    } else {
      os.RelPath(filename).baseName
    }

    if (whoami == "allyas") {
      println(command.showHelp)
      return 0
    }

    conf.get.get(whoami) match {
      case Some(cmd) =>
        if (config.verbose) {
          eprintln(s"*** Allyas: Executing command: $cmd")
        }
        val shell = sys.env.get("SHELL").getOrElse("sh")
        val proc = os
          .proc(shell, "-c", cmd)
          .call(
            cwd = os.pwd,
            stdin = os.Inherit,
            stdout = os.Inherit,
            stderr = os.Inherit
          )
        proc.exitCode
      case None =>
        eprintln(s"*** Allyas: Unknown command: $whoami")
        eprintln("Available commands:")
        conf.get.keys.foreach(eprintln)
        127
    }
  }

  def runShim(): Int = {
    if (Environments.ALLYAS_CONF.isEmpty) {
      eprintln("*** Allyas: Please set the ALLYAS_CONF environment variable.")
      return 1
    }

    if (Environments.ALLYAS_SHIM_DIR.isEmpty) {
      eprintln(
        "*** Allyas: Please set the ALLYAS_SHIM_DIR environment variable."
      )
      return 1
    }

    val confFile = Environments.ALLYAS_CONF.get
    val shimDir = Environments.ALLYAS_SHIM_DIR.get

    val confString =
      try {
        os.read(os.Path(confFile, os.pwd))
      } catch {
        case e: Exception =>
          eprintln(s"*** Allyas: Error reading config file: ${e.getMessage}")
          return 1
      }

    val parser = SExpressionParser()
    val conf = parser.parse(parser.config, confString)

    if (!conf.successful) {
      eprintln(s"*** Allyas: Error parsing config file: ${conf.toString}")
      return 1
    }

    val executablePath = if (filename.startsWith("/")) {
      os.Path(filename)
    } else {
      os.pwd / os.RelPath(filename)
    }
    val shimDirPath = os.Path(shimDir)

    try {
      if (!os.exists(shimDirPath)) {
        os.makeDir.all(shimDirPath)
        println(s"Created shim directory: $shimDir")
      }

      val createdLinks = conf.get.keys.map { aliasName =>
        val linkPath = shimDirPath / aliasName
        if (os.exists(linkPath)) {
          os.remove(linkPath)
        }
        os.symlink(linkPath, executablePath)
        aliasName
      }.toList

      println(s"Created ${createdLinks.size} symlinks in $shimDir:")
      createdLinks.foreach(name => println(s"  $name"))
      0
    } catch {
      case e: Exception =>
        eprintln(s"*** Allyas: Error creating symlinks: ${e.getMessage}")
        1
    }
  }
}
