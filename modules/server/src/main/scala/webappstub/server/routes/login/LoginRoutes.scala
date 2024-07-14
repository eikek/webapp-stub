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
import org.http4s.scalatags.*

final class LoginRoutes[F[_]: Async](
    login: LoginService[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]:

  private val api = LoginApi[F](login)

  def routes(ctx: Context.OptionalAuth) = HttpRoutes.of[F] {
    case GET -> Root =>
      // TODO: check for loggedin state
      Ok(Layout("Login", ctx.settings.theme)(View.view(uiCfg, ctx.settings), None))

    case req @ POST -> Root / "refresh" =>
      WebappstubAuth.fromRequest(req) match
        case None => BadRequest()
        case Some(token) =>
          api
            .refreshToken(token)
            .flatMap(
              _.fold(
                Forbidden(View.loginFailed("Authentication failed")),
                newToken => {
                  val cookie = WebappstubAuth.fromToken(newToken)
                  val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
                  NoContent().map(_.addCookie(cookie.asCookie(baseUrl)))
                }
              )
            )

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
