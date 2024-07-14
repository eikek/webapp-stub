package webappstub.server.routes.login

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.LoginService
import webappstub.server.Config
import webappstub.server.common.ClientRequestInfo
import webappstub.server.common.WebappstubAuth
import webappstub.server.context.Context
import webappstub.server.routes.{Layout, UiConfig}

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxLocation
import org.http4s.HttpRoutes
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.scalatags.*

final class LoginRoutes[F[_]: Async](
    login: LoginService[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]:

  private val api = LoginApi[F](login)

  def routes(ctx: Context.OptionalAuth) = HttpRoutes.of[F] {
    case GET -> Root =>
      if (ctx.token.isDefined) TemporaryRedirect(Location(uri"/app/contacts"))
      else Ok(Layout("Login", ctx.settings.theme)(View.view(uiCfg, ctx.settings)))

    case req @ POST -> Root =>
      for
        in <- req.as[Model.UserPasswordForm]
        result <- api.doLogin(in)
        resp <- result.fold(
          errs =>
            BadRequest(
              View.loginFailed(
                errs.errors.toList.flatMap(_.messages.toList).mkString(", ")
              )
            ),
          _.fold(
            Forbidden(View.loginFailed("Authentication failed")),
            token => {
              val cookie = WebappstubAuth.fromToken(token)
              val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
              val next = baseUrl / "app" / "contacts"
              NoContent()
                .map(_.addCookie(cookie.asCookie(baseUrl)))
                .map(_.putHeaders(HxLocation(HxLocation.Value.Path(next))))
            }
          )
        )
      yield resp
  }
