package webappstub.server.config

import scribe.Level

final case class LogConfig(
    minimumLevel: Level,
    format: LogConfig.Format,
    extraLevel: Map[String, Level]
)

object LogConfig:
  enum Format:
    case Plain
    case Fancy

  object Format:
    def fromString(s: String): Either[String, Format] =
      Format.values
        .find(_.productPrefix.equalsIgnoreCase(s))
        .toRight(s"Invalid log format: $s")
