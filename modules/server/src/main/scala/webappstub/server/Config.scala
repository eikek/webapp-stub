package webappstub.server

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.BackendConfig
import webappstub.server.config.*

import com.comcast.ip4s.*
import org.http4s.Uri

final case class Config(
    backend: BackendConfig,
    bindHost: Host,
    bindPort: Port,
    baseUrl: Uri,
    webapp: WebConfig
)

object Config:

  def load[F[_]: Async]: F[Config] =
    (
      ConfigValues.backend,
      ConfigValues.bindHost,
      ConfigValues.bindPort,
      ConfigValues.baseUri,
      ConfigValues.webConfig
    ).mapN(Config.apply).load[F]
