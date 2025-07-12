package dev.capslock.allyas

object Environments {
  val SHELL: String = sys.env.getOrElse("SHELL", "sh")
  val ALLYAS_CONF: Option[String] = sys.env.get("ALLYAS_CONF")
}
