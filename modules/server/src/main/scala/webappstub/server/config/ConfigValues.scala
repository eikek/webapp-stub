package webappstub.server.config

import cats.syntax.all.*
import ciris.*
import java.util.concurrent.atomic.AtomicReference
import webappstub.store.PostgresConfig
import com.comcast.ip4s.{Host, Port}
import webappstub.backend.BackendConfig
import webappstub.common.Password
import fs2.io.file.Path
import java.time.ZoneId

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

  val bindHost = config("BIND_HOST").default("0.0.0.0").as[Host]
  val bindPort = config("BIND_PORT").default("8888").as[Port]

  val backend = {
    postgres.map(BackendConfig.apply)
  }

  val timeZone =
    config("TIMEZONE", "Europe/Berlin").as[ZoneId]
