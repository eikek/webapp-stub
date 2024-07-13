package webappstub.server.config

import java.time.ZoneId

import scala.concurrent.duration.Duration

import cats.Show
import cats.syntax.all.*

import webappstub.common.model.Password
import webappstub.server.data.UiTheme

import ciris.*
import com.comcast.ip4s.*
import org.http4s.Uri
import scodec.bits.ByteVector

private trait ConfigDecoders:
  extension [A, B](self: ConfigDecoder[A, B])
    def emap[C](typeName: String)(f: B => Either[String, C])(using Show[B]) =
      self.mapEither((key, b) =>
        f(b).left.map(err => ConfigError.decode(typeName, key, b))
      )

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

  given ConfigDecoder[String, Uri] =
    ConfigDecoder[String].mapOption("Uri")(Uri.fromString(_).toOption)

  given ConfigDecoder[String, ByteVector] =
    ConfigDecoder[String].emap("ByteVector") { str =>
      if (str.startsWith("hex:"))
        ByteVector.fromHex(str.drop(4)).toRight(s"Invalid hex value: $str")
      else if (str.startsWith("b64:"))
        ByteVector.fromBase64(str.drop(4)).toRight(s"Invalid Base64 string: $str")
      else ByteVector.encodeUtf8(str).left.map(_.getMessage())
    }

  given ConfigDecoder[String, UiTheme] =
    ConfigDecoder[String].emap("UiTheme")(UiTheme.fromString)

  given ConfigDecoder[String, Duration] =
    ConfigDecoder[String].emap("Duration")(s =>
      Either.catchNonFatal(Duration(s)).leftMap(_.getMessage)
    )
