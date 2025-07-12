package dev.capslock.allyas

object Environments {
  val SHELL: String = sys.env.getOrElse("SHELL", "sh")
  val ALLY_CONF: Option[String] = sys.env.get("ALLY_CONF")
  val ALLY_SHIM_DIR: Option[String] = sys.env.get("ALLY_SHIM_DIR")
  val ALLY_VERBOSE: Boolean = sys.env.get("ALLY_VERBOSE").exists(_.toLowerCase == "true")
}
