package webappstub.server.config

import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.Duration

import cats.syntax.all.*
import fs2.io.file.Path

import webappstub.backend.BackendConfig
import webappstub.backend.auth.AuthConfig
import webappstub.backend.signup.{SignupConfig, SignupMode}
import webappstub.common.model.Password
import webappstub.store.PostgresConfig

import ciris.*
import com.comcast.ip4s.{Host, Port}
import org.http4s.Uri
import scodec.bits.ByteVector

object ConfigValues extends ConfigDecoders:
  private val envPrefix = "WEBAPPSTUB"
  private val values = new AtomicReference[Map[String, Option[String]]](Map.empty)
  private def addName(name: String, defaultValue: Option[String]) =
    values.updateAndGet(m => m.updated(name, defaultValue))

  private def config(
      name: String,
      default: Option[String]
  ): ConfigValue[Effect, String] = {
    val fullName = s"${envPrefix}_${name.toUpperCase}"
    addName(fullName, default)
    val propName = fullName.toLowerCase.replace('_', '.')
    val cv = prop(propName).or(env(fullName))
    default.map(cv.default(_)).getOrElse(cv)
  }

  private def config(name: String): ConfigValue[Effect, String] = config(name, None)

  private def config(name: String, defval: String): ConfigValue[Effect, String] =
    config(name, Some(defval))

  def getAll: Map[String, Option[String]] = values.get()

  lazy val userHome: Path =
    sys.props
      .get("user.home")
      .orElse(sys.env.get("HOME"))
      .map(Path.apply)
      .getOrElse(sys.error(s"No user home directory available!"))

  val postgres = (
    config("POSTGRES_HOST", "localhost").as[Host],
    config("POSTGRES_PORT", "5432").as[Port],
    config("POSTGRES_DATABASE", "webappstub"),
    config("POSTGRES_USER"),
    config("POSTGRES_PASSWORD").redacted.as[Password],
    config("POSTGRES_DEBUG", "false").as[Boolean],
    config("POSTGRES_MAX_CONNECTIONS", "8").as[Int]
  ).mapN(PostgresConfig.apply)

  val bindHost = config("BIND_HOST", "localhost").as[Host]
  val bindPort = config("BIND_PORT", "8888").as[Port]
  val baseUri = config("BASE_URI", "").as[Uri]

  val auth = {
    val secret = config("SERVER_SECRET", "hex:caffee").as[ByteVector]
    val valid = config("SESSION_VALID", "10 minutes").as[Duration]
    val authType = config("AUTH_FIXED", "false").as[Boolean].map {
      case true  => AuthConfig.AuthenticationType.Fixed
      case false => AuthConfig.AuthenticationType.Internal
    }
    (secret, valid, authType).mapN(AuthConfig.apply)
  }

  val signup = {
    val mode = config("SIGNUP_MODE", "closed").as[SignupMode]
    mode.map(SignupConfig.apply)
  }

  val backend =
    (postgres, auth, signup).mapN(BackendConfig.apply)

  val webConfig = {
    val name = config("WEB_APP_NAME", "Webappstub")
    name.map(WebConfig.apply)
  }

  val timeZone =
    config("TIMEZONE", "Europe/Berlin").as[ZoneId]
