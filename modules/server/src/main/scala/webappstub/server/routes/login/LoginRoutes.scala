package webappstub.server.routes.login

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.LoginService
import webappstub.server.Config
import webappstub.server.common.*
import webappstub.server.context.*
import webappstub.server.routes.{Layout, UiConfig}

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxLocation
import htmx4s.http4s.headers.HxRedirect
import org.http4s.*
import org.http4s.headers.Location
import org.http4s.implicits.*
import org.http4s.scalatags.*
import soidc.http4s.routes.JwtCookie

final class LoginRoutes[F[_]: Async](
    login: LoginService[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]:

  private val api = LoginApi[F](login)
  private val cookieName = "webappstub_auth"

  def routes = AuthedRoutes.of[MaybeAuthenticated, F] {
    case ContextRequest(ctx, req @ GET -> Root) =>
      val settings = Settings.fromRequest(req)
      if (ctx.token.isDefined) TemporaryRedirect(Location(uri"/app/contacts"))
      // else if (config.backend.auth.rememberMeEnabled) {
      //   import soidc.http4s.routes.*
      //   GetToken.cookie("weappstub_remember_me").
      // }
      else Ok(Layout("Login", settings.theme)(View.view(uiCfg, settings)))

    case ContextRequest(ctx, req @ POST -> Root) =>
      for
        in <- req.as[Model.UserPasswordForm]
        result <- api.doLogin(in)
        resp <- result.fold(
          errs => BadRequest(View.loginFailed(errs.mkString(", "))),
          _.fold(
            Forbidden(View.loginFailed("Authentication failed")),
            (token, rme) => {
              val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
              val cookie = JwtCookie.create(cookieName, token.jws, baseUrl)

              val next = baseUrl / "app" / "contacts"
              NoContent()
                .map(_.addCookie(cookie))
                .map(_.putHeaders(HxLocation(HxLocation.Value.Path(next))))
            }
          )
        )
      yield resp

    case ContextRequest(ctx, req @ DELETE -> Root) =>
      NoContent()
        .map(
          _.addCookie(
            JwtCookie.remove(cookieName, ClientRequestInfo.getBaseUrl(config, req))
          )
            .removeCookie(WebappstubRememberMe.cookieName)
            .putHeaders(HxRedirect(uri"/app/login"))
        )
  }
