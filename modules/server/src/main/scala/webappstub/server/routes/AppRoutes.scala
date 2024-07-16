package webappstub.server.routes

import cats.effect.*

import webappstub.backend.Backend
import webappstub.server.Config
import webappstub.server.common.Responses
import webappstub.server.context.ContextMiddleware
import webappstub.server.routes.contacts.ContactRoutes
import webappstub.server.routes.login.LoginRoutes
import webappstub.server.routes.settings.SettingRoutes
import webappstub.server.routes.signup.SignupRoutes

import htmx4s.http4s.Htmx4sDsl
import htmx4s.http4s.WebjarRoute
import htmx4s.http4s.WebjarRoute.Webjar
import org.http4s.HttpRoutes
import org.http4s.implicits.*
import org.http4s.server.Router

final class AppRoutes[F[_]: Async](backend: Backend[F], config: Config)
    extends Htmx4sDsl[F]:
  private def makeWebjar(uri: String, a: Webjars.Artifact, path: String = "") =
    Webjar(uri)(a.name, a.version, path)

  private val webjars = Seq(
    // refers to our own js and css stuff, version is not needed
    Webjar("self")("webappstub-server", "", ""),
    makeWebjar("htmx", Webjars.htmxorg, "dist"),
    makeWebjar("fa", Webjars.fortawesome__fontawesomefree),
    makeWebjar("fi", Webjars.flagicons)
  )

  val context = ContextMiddleware.forBackend[F](config, backend)
  val uiConfig = UiConfig.fromConfig(config)
  val login = LoginRoutes[F](backend.login, config, uiConfig)
  val signup = SignupRoutes[F](backend.signup, config, uiConfig)
  val contacts = ContactRoutes.create[F](backend)
  val settings = SettingRoutes[F](config)

  def routes: HttpRoutes[F] = Router.define(
    "/assets" -> WebjarRoute[F](webjars).serve,
    "/login" -> context.optional(login.routes),
    "/signup" -> context.optional(signup.routes),
    "/settings" -> context.optional(settings.routes),
    "/contacts" -> context.securedOrRedirect(contacts.routes, uri"/app/login")
  )(Responses.notFoundHtmlRoute)
