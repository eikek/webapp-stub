package webappstub.server.routes.login

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.Backend
import webappstub.server.Config
import webappstub.server.routes.UiConfig

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.headers.HxRedirect
import htmx4s.http4s.headers.HxRequest
import org.http4s.*
import org.http4s.headers.Location
import org.http4s.implicits.*
import soidc.core.ValidateFailure

final class LoginRoutes[F[_]: Async](
    backend: Backend[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]:
  private val logger = scribe.cats.effect[F]

  private val internalRoutes =
    InternalLoginRoutes[F](backend.realms, backend.login, config, uiCfg)
  private val openidRoutes = OpenIdLoginRoutes[F](backend, config, uiCfg)

  val routes = internalRoutes.routes <+> openidRoutes.routes

  val redirectToLogin: AuthedRoutes[ValidateFailure, F] =
    AuthedRoutes.of {
      case ContextRequest(err, req @ GET -> Root / "app" / "login") =>
        logger.warn(s"Token validation failed: $err") >>
          internalRoutes.renderLoginPage(req).map(Cookies.removeAuth(config, req))

      case ContextRequest(err, req) =>
        val target = uri"/app/login"
        val resp =
          if (req.headers.get[HxRequest].exists(_.flag)) NoContent()
          else SeeOther(Location(target))
        logger.warn(s"Token validation failed: $err") >>
          resp
            .map(Cookies.removeAuth(config, req))
            .map(_.putHeaders(HxRedirect(target)))
    }
