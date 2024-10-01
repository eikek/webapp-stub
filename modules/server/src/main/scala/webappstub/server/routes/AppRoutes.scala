package webappstub.server.routes

import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.Backend
import webappstub.backend.auth.WebappstubRealm
import webappstub.server.Config
import webappstub.server.common.Responses
import webappstub.server.context.AccountMiddleware
import webappstub.server.routes.contacts.ContactRoutes
import webappstub.server.routes.invite.InviteRoutes
import webappstub.server.routes.login.{AuthCookieName, LoginRoutes}
import webappstub.server.routes.settings.SettingRoutes
import webappstub.server.routes.signup.SignupRoutes

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.WebjarRoute
import htmx4s.http4s.WebjarRoute.Webjar
import org.http4s.HttpRoutes
import org.http4s.implicits.*
import org.http4s.server.Router
import soidc.borer.given
import soidc.http4s.routes.GetToken
import soidc.http4s.routes.JwtAuthMiddleware
import soidc.jwt.JoseHeader
import soidc.jwt.SimpleClaims
import org.http4s.Response
import org.http4s.Request
import org.http4s.headers.Location

final class AppRoutes[F[_]: Async](backend: Backend[F], config: Config)
    extends Htmx4sDsl[F]:
  private val logger = scribe.cats.effect[F]
  private def makeWebjar(uri: String, a: Webjars.Artifact, path: String = "") =
    Webjar(uri)(a.name, a.version, path)

  private val webjars = Seq(
    // refers to our own js and css stuff, version is not needed
    Webjar("self")("webappstub-server", "", ""),
    makeWebjar("htmx", Webjars.htmxorg, "dist"),
    makeWebjar("htmx-rt", Webjars.htmxextresponsetargets),
    makeWebjar("fa", Webjars.fortawesome__fontawesomefree),
    makeWebjar("fi", Webjars.flagicons)
  )

  val makeRealm =
    backend.login
      .openIdRealms(uri"")
      .map(_.values.toSeq)
      .map(vs => backend.login.internalRealm +: vs)
      .map(_.combineAll)

  val uiConfig = UiConfig.fromConfig(config)
  val login = LoginRoutes[F](backend.login, backend.signup, config, uiConfig)
  val signup = SignupRoutes[F](backend.signup, config, uiConfig)
  val invite = InviteRoutes[F](backend.signup)
  val contacts = ContactRoutes.create[F](backend)
  val settings = SettingRoutes[F](config)

  val redirectToLogin: Request[F] => F[Response[F]] = { req =>
    val target = uri"/app/login"
    (if (req.uri.path == target.path) login.renderLoginPage(req)
     else SeeOther(Location(target))).map(login.removeCookies(req))
  }

  def withJwtAuth(realm: WebappstubRealm[F]) = JwtAuthMiddleware
    .builder[F, JoseHeader, SimpleClaims]
    .withGeToken(GetToken.anyOf(GetToken.bearer, GetToken.cookie("webappstub_auth")))
    .withOnInvalidToken(err => logger.warn(s"Token validation error: $err"))
    .withValidator(realm.validator)
    .withRefresh(
      realm.jwtRefresh,
      _.updateCookie(AuthCookieName.value.asString, uri"")
    )
    .withOnFailure(redirectToLogin)

  val withAccount = AccountMiddleware
    .builder[F]
    .withLookup(backend.accountRepo.findByKey)

  def routes: F[HttpRoutes[F]] = makeRealm.map { realm =>
    val withJwt = withJwtAuth(realm)
    Router.define(
      "/assets" -> WebjarRoute[F](webjars).serve,
      "/login" -> withJwt.securedOrAnonymous(login.routes),
      "/signup" -> withJwt.securedOrAnonymous(signup.routes),
      "/create-invite" -> withJwt.secured(withAccount.required(invite.routes)),
      "/settings" -> withJwt.securedOrAnonymous(settings.routes),
      "/contacts" -> withJwt.secured(withAccount.required(contacts.routes))
    )(Responses.notFoundHtmlRoute)
  }
