package webappstub.server.routes.settings

import cats.effect.*
import cats.syntax.all.*

import webappstub.server.Config
import webappstub.server.common.*
import webappstub.server.context.*
import webappstub.server.data.UiTheme

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxRefresh
import org.http4s.*
import org.http4s.scalatags.*

final class SettingRoutes[F[_]: Async](config: Config) extends Htmx4sDsl[F]:

  def routes = AuthedRoutes.of[MaybeAuthenticated, F] {
    case ContextRequest(_, req @ POST -> Root / "cycle-theme") =>
      val settings = Settings.fromRequest(req)
      val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
      val next = UiTheme.cycle(settings.theme)
      NoContent().map(
        _.addCookie(WebappstubTheme(next).asCookie(baseUrl)).putHeaders(HxRefresh(true))
      )

    case ContextRequest(_, req @ POST -> Root / "language") =>
      for
        in <- req.as[Model.SettingsForm]
        baseUrl = ClientRequestInfo.getBaseUrl(config, req)
        resp <-
          NoContent().map { r =>
            in.language
              .map(lang =>
                r.addCookie(lang.asCookie(baseUrl))
                  .putHeaders(HxRefresh(true))
              )
              .getOrElse(r)
          }
      yield resp

    case ContextRequest(_, req @ GET -> Root / "language" :? Params.MenuOpen(flag)) =>
      val settings = Settings.fromRequest(req)
      Ok(Views.languageDropdown(settings.language, flag))

    case ContextRequest(_, req @ POST -> Root) =>
      val settings = Settings.fromRequest(req)
      for
        in <- req.as[Model.SettingsForm]

        baseUrl = ClientRequestInfo.getBaseUrl(config, req)
        theme = in.theme.getOrElse(WebappstubTheme(settings.theme))
        lang = in.language.getOrElse(WebappstubLang(settings.language))

        resp <- NoContent().map(
          _.addCookie(theme.asCookie(baseUrl))
            .addCookie(lang.asCookie(baseUrl))
            .putHeaders(HxRefresh(true))
        )
      yield resp
  }
