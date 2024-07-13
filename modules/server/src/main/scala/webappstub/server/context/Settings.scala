package webappstub.server.context

import webappstub.server.common.*
import webappstub.server.data.*

import org.http4s.Request

final case class Settings(
    theme: UiTheme,
    language: UiLanguage
)

object Settings:
  val default = Settings(
    UiTheme.Light,
    UiLanguage.English
  )

  def fromRequest[F[_]](req: Request[F]) =
    Settings(
      WebappstubTheme.fromRequest(req).map(_.theme).getOrElse(default.theme),
      WebappstubLang.fromRequest(req).map(_.lang).getOrElse(default.language)
    )
