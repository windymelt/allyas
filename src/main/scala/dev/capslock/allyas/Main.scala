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
    configFile: Option[String]
)

object Main {
  val command = Command(
    name = "ally",
    header = "A command aliasing tool"
  )(Opts.unit)

  def main(args: Array[String]): Unit = {
    val whoami = if (filename.startsWith("/")) {
      os.Path(filename).baseName
    } else {
      os.RelPath(filename).baseName
    }

    if (args.length > 0 && args(0) == "shim") {
      val exitCode = runShim()
      sys.exit(exitCode)
    } else if (whoami == "ally") {
      command.parse(args.toList) match {
        case Left(help) =>
          eprintln(help.toString)
          sys.exit(1)
        case Right(_) =>
          val config = Config(Environments.ALLY_CONF)
          val exitCode = run(config, Array.empty)
          sys.exit(exitCode)
      }
    } else {
      val config = Config(Environments.ALLY_CONF)
      val exitCode = run(config, args)
      sys.exit(exitCode)
    }
  }

  def run(config: Config, args: Array[String] = Array.empty): Int = {
    val confFile = config.configFile match {
      case Some(file) => file
      case None =>
        eprintln("*** Ally: Please set the ALLY_CONF environment variable.")
        return 1
    }

    if (Environments.ALLY_VERBOSE) {
      eprintln(s"*** Ally: Using config file: $confFile")
    }

    val confString =
      try {
        os.read(os.Path(confFile, os.pwd))
      } catch {
        case e: Exception =>
          eprintln(s"*** Ally: Error reading config file: ${e.getMessage}")
          return 1
      }

    val parser = SExpressionParser()
    val conf = parser.parse(parser.config, confString)

    if (!conf.successful) {
      eprintln(s"*** Ally: Error parsing config file: ${conf.toString}")
      return 1
    }

    val whoami = if (filename.startsWith("/")) {
      os.Path(filename).baseName
    } else {
      os.RelPath(filename).baseName
    }

    conf.get.get(whoami) match {
      case Some(cmd) =>
        val quotedArgs = args.map(arg => s"'${arg.replace("'", "'\\''")}'")
        val fullCommand = if (quotedArgs.nonEmpty) {
          s"$cmd ${quotedArgs.mkString(" ")}"
        } else {
          cmd
        }

        if (Environments.ALLY_VERBOSE) {
          eprintln(s"*** Ally: Executing command: $fullCommand")
        }
        val shell = sys.env.get("SHELL").getOrElse("sh")
        val proc = os
          .proc(shell, "-c", fullCommand)
          .call(
            cwd = os.pwd,
            stdin = os.Inherit,
            stdout = os.Inherit,
            stderr = os.Inherit
          )
        proc.exitCode
      case None =>
        eprintln(s"*** Ally: Unknown command: $whoami")
        eprintln("Available commands:")
        conf.get.keys.foreach(eprintln)
        127
    }
  }

  def runShim(): Int = {
    if (Environments.ALLY_CONF.isEmpty) {
      eprintln("*** Ally: Please set the ALLY_CONF environment variable.")
      return 1
    }

    if (Environments.ALLY_SHIM_DIR.isEmpty) {
      eprintln(
        "*** Ally: Please set the ALLY_SHIM_DIR environment variable."
      )
      return 1
    }

    val confFile = Environments.ALLY_CONF.get
    val shimDir = Environments.ALLY_SHIM_DIR.get

    val confString =
      try {
        os.read(os.Path(confFile, os.pwd))
      } catch {
        case e: Exception =>
          eprintln(s"*** Ally: Error reading config file: ${e.getMessage}")
          return 1
      }

    val parser = SExpressionParser()
    val conf = parser.parse(parser.config, confString)

    if (!conf.successful) {
      eprintln(s"*** Ally: Error parsing config file: ${conf.toString}")
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
        eprintln(s"*** Ally: Error creating symlinks: ${e.getMessage}")
        1
    }
  }
}
