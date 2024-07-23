package webappstub.server.routes

import webappstub.server.Config

final case class UiConfig(
    name: String,
    assetPath: String,
    rememberMeEnabled: Boolean
)

object UiConfig:
  def fromConfig(cfg: Config): UiConfig =
    UiConfig(cfg.webapp.appName, "/app/assets", cfg.backend.auth.rememberMeEnabled)
