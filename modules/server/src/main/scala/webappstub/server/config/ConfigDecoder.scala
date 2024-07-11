package webappstub.server.config

import cats.syntax.all.*
import ciris.*
import webappstub.common.Password
import com.comcast.ip4s.*
import java.time.ZoneId

trait ConfigDecoders:

  given ConfigDecoder[String, Password] =
    ConfigDecoder[String].map(Password.apply)

  given ConfigDecoder[String, Host] =
    ConfigDecoder[String].mapOption("host")(Host.fromString)

  given ConfigDecoder[String, Port] =
    ConfigDecoder[String].mapOption("port")(Port.fromString)

  given ConfigDecoder[String, ZoneId] =
    ConfigDecoder[String].mapOption("TimeZone") { s =>
      if (ZoneId.getAvailableZoneIds.contains(s)) ZoneId.of(s).some
      else None
    }
