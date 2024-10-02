package webappstub.server.routes.login

import cats.data.Validated
import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.LoginService
import webappstub.backend.auth.*
import webappstub.common.model.*
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
import soidc.borer.given
import soidc.http4s.client.ByteEntityDecoder
import soidc.http4s.routes.JwtAuth
import soidc.jwt.JoseHeader
import soidc.jwt.SimpleClaims

final class InternalLoginRoutes[F[_]: Async](
    login: LoginService[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]
    with ByteEntityDecoder:

  private val rememberMeAuth = JwtAuth
    .builder[F, JoseHeader, SimpleClaims]
    .withGetToken(RememberMeCookie.getToken)
    .withValidator(login.rememberMeValidator)
    .securedOrAnonymous

  def findRememberMe(req: Request[F]): F[Option[RememberMeToken]] =
    rememberMeAuth(req).subflatMap(_.getToken).value

  def renderLoginPage(req: Request[F]): F[Response[F]] =
    val settings = Settings.fromRequest(req)
    Ok(Layout("Login", settings.theme)(View.view(uiCfg, settings)))

  def routes = AuthedRoutes.of[MaybeAuthenticated, F] {
    case ContextRequest(ctx, req @ DELETE -> Root) =>
      NoContent()
        .map(Cookies.removeAll(config)(req))
        .map(_.putHeaders(HxRedirect(uri"/app/login")))

    case ContextRequest(ctx, req @ GET -> Root) =>
      if (ctx.isAuthenticated) Found(Location(uri"/app/contacts"))
      else
        findRememberMe(req).flatMap {
          case None => renderLoginPage(req)
          case Some(rkey) =>
            login.loginRememberMe(rkey).flatMap {
              case LoginResult.Success(token, _) =>
                val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
                val cookie = AuthCookie.set(token, baseUrl)
                Found(Location(uri"/app/contacts")).map(_.addCookie(cookie))
              case _ =>
                renderLoginPage(req)
            }
        }

    case ContextRequest(ctx, req @ POST -> Root) =>
      for
        in <- req.as[Model.UserPasswordForm]
        result <- in.toModel match {
          case e @ Validated.Invalid(_) => e.pure[F]
          case Validated.Valid(up)      => login.loginInternal(up).map(_.valid)
        }
        resp <- result.fold(
          errs => BadRequest(View.loginFailed(errs.mkString(", "))),
          _.fold(
            Forbidden(View.loginFailed("Authentication failed")),
            (token, rme) => {
              val resp = NoContent().map(
                _.putHeaders(HxLocation(HxLocation.Value.Path(uri"/app/contacts")))
              )
              Cookies.set(config)(req, token, rme)(resp)
            }
          )
        )
      yield resp
  }
