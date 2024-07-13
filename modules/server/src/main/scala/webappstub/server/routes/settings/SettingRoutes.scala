package webappstub.server.routes.settings

import cats.effect.*
import cats.syntax.all.*

import webappstub.server.Config
import webappstub.server.common.*
import webappstub.server.context.Context
import webappstub.server.data.UiTheme

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxRefresh
import org.http4s.HttpRoutes
import org.http4s.scalatags.*

final class SettingRoutes[F[_]: Async](config: Config) extends Htmx4sDsl[F]:

  def routes(ctx: Context.OptionalAuth) = HttpRoutes.of[F] {
    case req @ POST -> Root / "cycle-theme" =>
      val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
      val next = UiTheme.cycle(ctx.settings.theme)
      NoContent().map(
        _.addCookie(WebappstubTheme(next).asCookie(baseUrl)).putHeaders(HxRefresh(true))
      )

    case req @ POST -> Root / "language" =>
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

    case GET -> Root / "language" :? Params.MenuOpen(flag) =>
      Ok(Views.languageDropdown(ctx.settings.language, flag))

    case req @ POST -> Root =>
      for
        in <- req.as[Model.SettingsForm]

        baseUrl = ClientRequestInfo.getBaseUrl(config, req)
        theme = in.theme.getOrElse(WebappstubTheme(ctx.settings.theme))
        lang = in.language.getOrElse(WebappstubLang(ctx.settings.language))

        resp <- NoContent().map(
          _.addCookie(theme.asCookie(baseUrl))
            .addCookie(lang.asCookie(baseUrl))
            .putHeaders(HxRefresh(true))
        )
      yield resp
  }
