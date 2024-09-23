package webappstub.server.routes.login

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.LoginService
import webappstub.backend.auth.LoginResult
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
  private val rememberMeCookie = api.rememberMeCookie

  def routes = AuthedRoutes.of[MaybeAuthenticated, F] {
    case ContextRequest(ctx, req @ GET -> Root) =>
      val settings = Settings.fromRequest(req)
      if (ctx.isAuthenticated) TemporaryRedirect(Location(uri"/app/contacts"))
      else
        api.findRememberMe(req).flatMap {
          case None => Ok(Layout("Login", settings.theme)(View.view(uiCfg, settings)))
          case Some(rkey) =>
            api.doRememberMeLogin(rkey).flatMap {
              case LoginResult.Success(token, _) =>
                val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
                val cookie = JwtCookie
                  .create(cookieName, token.jws, baseUrl)
                  .copy(maxAge = token.claims.expirationTime.map(_.toSeconds))
                TemporaryRedirect(Location(uri"/app/contacts")).map(_.addCookie(cookie))
              case LoginResult.InvalidAuth =>
                Ok(Layout("Login", settings.theme)(View.view(uiCfg, settings)))
            }
        }

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
              val cookie = JwtCookie
                .create(cookieName, token.jws, baseUrl)
                .copy(maxAge = token.claims.expirationTime.map(_.toSeconds))
              val rmeCookie = rme
                .map(t =>
                  JwtCookie
                    .create(rememberMeCookie, t.jws, baseUrl)
                    .copy(maxAge = t.claims.expirationTime.map(_.toSeconds))
                )
                .getOrElse(JwtCookie.remove(rememberMeCookie, baseUrl))

              val next = baseUrl / "app" / "contacts"
              NoContent()
                .map(_.addCookie(cookie).addCookie(rmeCookie))
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
            .addCookie(
              JwtCookie
                .remove(rememberMeCookie, ClientRequestInfo.getBaseUrl(config, req))
            )
            .putHeaders(HxRedirect(uri"/app/login"))
        )
  }
