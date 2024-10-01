package webappstub.server.routes.login

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.{LoginService, SignupService}
import webappstub.server.Config
import webappstub.server.routes.UiConfig

final class LoginRoutes[F[_]: Async](
    login: LoginService[F],
    signup: SignupService[F],
    config: Config,
    uiCfg: UiConfig
):

  private val internalRoutes = InternalLoginRoutes[F](login, config, uiCfg)
  private val openidRoutes = OpenIdLoginRoutes[F](login, signup, config, uiCfg)

  val routes = internalRoutes.routes <+> openidRoutes.routes

  def renderLoginPage = internalRoutes.renderLoginPage
  def removeCookies = Cookies.remove[F](config)
