package webappstub.server.config

import java.time.ZoneId
import java.util.concurrent.atomic.AtomicReference

import scala.concurrent.duration.*

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
import scribe.Level
import soidc.core.model.*
import soidc.jwt.{JWK, Uri as JwtUri}

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

  def envMap[A, B](
      envName: String
  )(using ConfigDecoder[String, A], ConfigDecoder[String, B]) =
    config(s"${envName}_NAMES")
      .as[List[String]]
      .listflatMap { k =>
        val value = config(s"${envName}_$k").as[B]
        val ckey = ConfigKey(s"${envName} key: $k")
        val kk = ConfigDecoder[String, A]
          .decode(Some(ckey), k)
          .fold(ConfigValue.failed, ConfigValue.loaded(ckey, _))

        value.flatMap(v => kk.map(_ -> v))
      }
      .map(_.toMap)

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
    config("POSTGRES_MAX_CONNECTIONS", "8").as[Int],
    config("POSTGRES_CONNECT_RETRY_DELAY", "10 seconds").as[FiniteDuration]
  ).mapN(PostgresConfig.apply)

  val bindHost = config("BIND_HOST", "localhost").as[Host]
  val bindPort = config("BIND_PORT", "8888").as[Port]
  val baseUri = config("BASE_URI", "").as[Uri]

  val auth = {
    val intEnabled = config("AUTH_INTERNAL_ENABLED", "true").as[Boolean]
    val secret = config("SERVER_SECRET").as[JWK].option
    val valid = config("AUTH_INTERNAL_SESSION_VALID", "10 minutes").as[FiniteDuration]
    val rememberValid =
      config("AUTH_INTERNAL_REMEMBER_ME_VALID", "30 days").as[FiniteDuration]
    val internal =
      (intEnabled, secret, valid, rememberValid).mapN(AuthConfig.Internal.apply)
    val openIdProvider = config("AUTH_OPENID_PROVIDERS", "").as[List[String]]
    val openId = openIdProvider.listflatMap(openIdConfig)
    (internal, openId).mapN((a, b) => AuthConfig(a, b.toMap))
  }

  private def openIdConfig(name: String) = {
    val n = name.toUpperCase()
    val providerUri = config(s"OPENID_${n}_PROVIDER_URI").as[JwtUri]
    val clientId = config(s"OPENID_${n}_CLIENT_ID").as[ClientId]
    val clientSecret = config(s"OPENID_${n}_CLIENT_SECRET").as[ClientSecret]
    val scopes = config(s"OPENID_${n}_SCOPE").as[ScopeList].option
    (providerUri, clientId, clientSecret, scopes)
      .mapN((a, b, c, d) => name -> AuthConfig.OpenId(a, b, c, d))
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

  val logConfig = {
    val minLevel = config("LOGGING_MIN_LEVEL", "ERROR").as[Level]
    val fmt = config("LOGGING_FORMAT", "plain").as[LogConfig.Format]
    val extraLevel = envMap[String, Level]("LOGGING_LEVELS").default(Map.empty)
    (minLevel, fmt, extraLevel).mapN(LogConfig.apply)
  }

  val timeZone =
    config("TIMEZONE", "Europe/Berlin").as[ZoneId]
