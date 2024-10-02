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

final class LoginRoutes[F[_]: Async](
    backend: Backend[F],
    config: Config,
    uiCfg: UiConfig
) extends Htmx4sDsl[F]:

  private val internalRoutes =
    InternalLoginRoutes[F](backend.realms, backend.login, config, uiCfg)
  private val openidRoutes = OpenIdLoginRoutes[F](backend, config, uiCfg)

  val routes = internalRoutes.routes <+> openidRoutes.routes

  val redirectToLogin: Request[F] => F[Response[F]] = {
    case req @ GET -> Root / "app" / "login" =>
      internalRoutes.renderLoginPage(req).map(Cookies.removeAuth(config, req))
    case req =>
      val target = uri"/app/login"
      val resp =
        if (req.headers.get[HxRequest].exists(_.flag)) NoContent()
        else SeeOther(Location(target))
      resp
        .map(Cookies.removeAuth(config, req))
        .map(_.putHeaders(HxRedirect(target)))
  }
