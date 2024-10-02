package webappstub.server.routes

import cats.effect.*

import webappstub.backend.Backend
import webappstub.backend.auth.WebappstubRealm
import webappstub.server.Config
import webappstub.server.common.Responses
import webappstub.server.context.AccountMiddleware
import webappstub.server.routes.contacts.ContactRoutes
import webappstub.server.routes.invite.InviteRoutes
import webappstub.server.routes.login.{AuthCookie, LoginRoutes}
import webappstub.server.routes.settings.SettingRoutes
import webappstub.server.routes.signup.SignupRoutes

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.WebjarRoute
import org.http4s.*
import org.http4s.server.Router
import soidc.borer.given
import soidc.http4s.routes.GetToken
import soidc.http4s.routes.JwtAuthMiddleware
import soidc.jwt.JoseHeader
import soidc.jwt.SimpleClaims

final class AppRoutes[F[_]: Async](backend: Backend[F], config: Config)
    extends Htmx4sDsl[F]:
  private val logger = scribe.cats.effect[F]

  val uiConfig = UiConfig.fromConfig(config)
  val login = LoginRoutes[F](backend, config, uiConfig)
  val signup = SignupRoutes[F](backend.signup, config, uiConfig)
  val invite = InviteRoutes[F](backend.signup)
  val contacts = ContactRoutes.create[F](backend)
  val settings = SettingRoutes[F](config)

  def withJwtAuth(realm: WebappstubRealm[F]) = JwtAuthMiddleware
    .builder[F, JoseHeader, SimpleClaims]
    .withGeToken(GetToken.anyOf(GetToken.bearer, GetToken.cookie("webappstub_auth")))
    .withOnInvalidToken(err => logger.warn(s"Token validation error: $err"))
    .withValidator(realm.validator)
    .withRefresh(
      realm.jwtRefresh,
      _.updateCookie(AuthCookie.value.asString, config.baseUrl)
    )
    .withOnFailure(login.redirectToLogin)

  val withAccount = AccountMiddleware
    .builder[F]
    .withLookup(backend.accountRepo.findByKey)

  def routes: F[HttpRoutes[F]] = Async[F].pure {
    val realm = backend.realms
    val withJwt = withJwtAuth(realm)
    Router.define(
      "/assets" -> WebjarRoute[F](WebjarDef.webjars).serve,
      "/login" -> withJwt.securedOrAnonymous(login.routes),
      "/signup" -> withJwt.securedOrAnonymous(signup.routes),
      "/create-invite" -> withJwt.secured(withAccount.required(invite.routes)),
      "/settings" -> withJwt.securedOrAnonymous(settings.routes),
      "/contacts" -> withJwt.secured(withAccount.required(contacts.routes))
    )(Responses.notFoundHtmlRoute)
  }
