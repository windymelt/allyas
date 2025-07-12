package dev.capslock.allyas

object Environments {
  val SHELL: String = sys.env.getOrElse("SHELL", "sh")
  val ALLYAS_CONF: Option[String] = sys.env.get("ALLYAS_CONF")
  val ALLYAS_SHIM_DIR: Option[String] = sys.env.get("ALLYAS_SHIM_DIR")
}
