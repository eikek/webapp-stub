package webappstub.server

import cats.syntax.all.*
import cats.effect.*
import com.comcast.ip4s.*
import webappstub.server.config.*
import webappstub.backend.BackendConfig

final case class Config(
  backend: BackendConfig,
  bindHost: Host,
  bindPort: Port
)

object Config:

  def load[F[_]: Async]: F[Config] =
      (ConfigValues.backend,
      ConfigValues.bindHost,
      ConfigValues.bindPort).mapN(Config.apply).load[F]
