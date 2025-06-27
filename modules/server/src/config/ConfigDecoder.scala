package webappstub.server.config

import java.time.ZoneId

import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration

import cats.Show
import cats.syntax.all.*

import webappstub.backend.signup.SignupMode
import webappstub.common.model.Password
import webappstub.server.data.UiTheme

import ciris.*
import com.comcast.ip4s.*
import org.http4s.Uri
import scodec.bits.ByteVector
import scribe.Level
import soidc.core.model.*
import soidc.jwt.{Uri as JwtUri, *}

private trait ConfigDecoders:
  extension [A, B](self: ConfigDecoder[A, B])
    def emap[C](typeName: String)(f: B => Either[String, C])(using Show[B]) =
      self.mapEither((key, b) => f(b).left.map(_ => ConfigError.decode(typeName, key, b)))

  extension [A](self: ConfigValue[Effect, List[A]])
    def listflatMap[B](f: A => ConfigValue[Effect, B]): ConfigValue[Effect, List[B]] =
      self.flatMap(ids =>
        ids.foldLeft(ConfigValue.loaded(ConfigKey(""), List.empty[B])) { (cv, id) =>
          cv.flatMap(l => f(id).map(_ :: l))
        }
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

  given ConfigDecoder[String, JWK] =
    ConfigDecoder[String, ByteVector].map(bv => JWK.symmetric(bv, Algorithm.Sign.HS256))

  given ConfigDecoder[String, JwtUri] =
    ConfigDecoder[String].emap("JwtUri")(JwtUri.fromString)

  given ConfigDecoder[String, ClientId] =
    ConfigDecoder[String].map(ClientId.apply)

  given ConfigDecoder[String, ClientSecret] =
    ConfigDecoder[String].map(ClientSecret.apply)

  given ConfigDecoder[String, ScopeList] =
    ConfigDecoder[String].map(ScopeList.fromString)

  given ConfigDecoder[String, UiTheme] =
    ConfigDecoder[String].emap("UiTheme")(UiTheme.fromString)

  given ConfigDecoder[String, Duration] =
    ConfigDecoder[String].emap("Duration")(s =>
      Either.catchNonFatal(Duration(s)).leftMap(_.getMessage)
    )

  given ConfigDecoder[String, FiniteDuration] =
    ConfigDecoder[String, Duration].mapOption("FiniteDuration") {
      case d: FiniteDuration => Some(d)
      case _                 => None
    }

  given ConfigDecoder[String, SignupMode] =
    ConfigDecoder[String].emap("SignupMode")(SignupMode.fromString)

  given ConfigDecoder[String, Level] =
    ConfigDecoder[String].mapOption("Level")(Level.get)

  given ConfigDecoder[String, LogConfig.Format] =
    ConfigDecoder[String].emap("LogFormat")(LogConfig.Format.fromString)

  given [A](using ConfigDecoder[String, A]): ConfigDecoder[String, List[A]] =
    ConfigDecoder[String].mapEither { (ckey, str) =>
      str
        .split(',')
        .toList
        .map(_.trim)
        .filter(_.nonEmpty)
        .traverse(
          ConfigDecoder[String, A].decode(ckey, _)
        )
    }
