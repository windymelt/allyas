package dev.capslock.allyas

import scala.scalanative.unsafe.Zone
import scalanative.runtime.filename
import scala.util.Success
import scala.util.Failure
import scala.util.parsing.combinator.Parsers

inline def eprintln(msg: String): Unit = Console.err.println(msg)

object Main {
  def main(args: Array[String]): Unit = {
    val exitCode = run(args)
    sys.exit(exitCode)
  }

  def run(args: Array[String]): Int = {
    if (Environments.ALLYAS_CONF.isEmpty) {
      eprintln(
        "*** Allyas: Please set the ALLYAS_CONF environment variable."
      )
      return 1
    }
    val confFile = Environments.ALLYAS_CONF.get
    val confString = os.read(os.Path(confFile, os.pwd))
    val parser = SExpressionParser()
    val conf =
      parser.parse(parser.config, confString)

    if (!conf.successful) {
      eprintln(s"*** Allyas: Error parsing config file: ${conf.toString}")
      return 1
    }

    val whoami = os.RelPath(filename).baseName
    conf.get.get(whoami) match {
      case Some(cmd) =>
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
        conf.get.keys.foreach(println)
        127
    }
  }
}
// TODO: autolinking
