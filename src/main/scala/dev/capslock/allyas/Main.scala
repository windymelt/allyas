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

    val confString = try {
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

    val whoami = os.RelPath(filename).baseName
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
}
// TODO: autolinking
