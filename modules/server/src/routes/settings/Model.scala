package webappstub.server.routes.settings

import cats.syntax.all.*

import webappstub.server.common.*

import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*
import org.http4s.QueryParamDecoder

object Model:
  final case class SettingsForm(
      theme: Option[WebappstubTheme],
      language: Option[WebappstubLang]
  )

  object SettingsForm:
    given QueryParamDecoder[WebappstubLang] =
      QueryParamDecoder[String].emap(WebappstubLang.parse)

    given QueryParamDecoder[WebappstubTheme] =
      QueryParamDecoder[String].emap(WebappstubTheme.parse)

    given FormDataDecoder[SettingsForm] =
      (fieldOptional[WebappstubTheme]("theme"), fieldOptional[WebappstubLang]("language"))
        .mapN(SettingsForm.apply)
