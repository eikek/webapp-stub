package webappstub.store

import scala.concurrent.duration.FiniteDuration

import webappstub.common.model.Password

import com.comcast.ip4s.{Host, Port}
import io.bullet.borer.Encoder
import io.bullet.borer.derivation.MapBasedCodecs.*

final case class PostgresConfig(
    host: Host,
    port: Port,
    database: String,
    user: Option[PostgresConfig.User],
    debug: Boolean,
    maxConnections: Int,
    connectRetryDelay: FiniteDuration
)

object PostgresConfig:
  final case class User(username: String, password: Option[Password])

  object User:
    given Encoder[User] = deriveEncoder

  private given Encoder[FiniteDuration] = Encoder.forString.contramap(_.toString())
  given Encoder[Host] = Encoder.forString.contramap(_.toString)
  given Encoder[Port] = Encoder.forInt.contramap(_.value)
  given Encoder[PostgresConfig] = deriveEncoder
