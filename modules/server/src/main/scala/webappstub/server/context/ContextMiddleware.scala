package webappstub.server.context

import cats.Monad
import cats.data.Kleisli
import cats.data.OptionT
import cats.syntax.all.*

import webappstub.backend.Backend
import webappstub.backend.auth.AuthConfig
import webappstub.backend.auth.AuthConfig.AuthenticationType
import webappstub.backend.auth.LoginResult
import webappstub.server.Config
import webappstub.server.common.ClientRequestInfo
import webappstub.server.common.WebappstubAuth

import org.http4s.*
import org.http4s.Request
import org.http4s.headers.Location
import org.http4s.server.AuthMiddleware

trait ContextMiddleware[F[_]]:
  /** Runs inner routes for authenticated requests only, otherwise an error repsonse is
    * generated.
    */
  def secured(
      inner: Context.Authenticated => HttpRoutes[F],
      onFailure: Request[F] => F[Response[F]]
  ): HttpRoutes[F]

  /** Runs inner routes for authenticated requests only, otherwise a redirect response is
    * generated
    */
  def securedOrRedirect(
      inner: Context.Authenticated => HttpRoutes[F],
      uri: Uri
  ): HttpRoutes[F]

  /** Always runs inner routes, given access to authenticated information if available. */
  def optional(inner: Context.OptionalAuth => HttpRoutes[F]): HttpRoutes[F]

object ContextMiddleware:
  type GetContext[F[_], A] = Kleisli[[X] =>> OptionT[F, X], Request[F], A]

  private def getContext[F[_]: Monad](
      login: Option[String] => F[LoginResult]
  ): GetContext[F, Context.Authenticated] =
    Kleisli { (req: Request[F]) =>
      OptionT(
        login(WebappstubAuth.fromRequest(req).map(_.token)).map {
          case LoginResult.Success(token) =>
            Some(Context.Authenticated(token, Settings.fromRequest(req)))

          case _ =>
            None
        }
      )
    }

  def forBackend[F[_]: Monad](cfg: Config, backend: Backend[F]) =
    backend.config.auth.authType match
      case AuthenticationType.Fixed =>
        apply(cfg) {
          case Some(token) =>
            backend.login.loginSession(token).flatMap {
              case r: LoginResult.Success => r.pure[F]
              case _                      => backend.login.autoLogin
            }
          case None => backend.login.autoLogin
        }
      case AuthenticationType.Internal =>
        apply(cfg) {
          case Some(token) => backend.login.loginSession(token)
          case None        => LoginResult.InvalidAuth.pure[F]
        }

  def apply[F[_]: Monad](
      cfg: Config
  )(login: Option[String] => F[LoginResult]): ContextMiddleware[F] =
    new ContextMiddleware[F] {
      def secured(
          inner: Context.Authenticated => HttpRoutes[F],
          onFailure: Request[F] => F[Response[F]]
      ): HttpRoutes[F] =
        AuthMiddleware
          .noSpider(getContext(login), onFailure)
          .apply(Kleisli(req => inner(req.context)(req.req).withAuthCookie(req)))

      def securedOrRedirect(
          inner: Context.Authenticated => HttpRoutes[F],
          uri: Uri
      ): HttpRoutes[F] =
        secured(
          inner,
          _ =>
            Response(status = Status.TemporaryRedirect, headers = Headers(Location(uri)))
              .pure[F]
        )

      def optional(
          inner: Context.OptionalAuth => HttpRoutes[F]
      ): HttpRoutes[F] = {
        val ctxAuth = getContext(login).map(_.toOptional)
        val ctxNoAuth: GetContext[F, Context.OptionalAuth] =
          Kleisli(req =>
            OptionT.pure(Context.OptionalAuth(None, Settings.fromRequest(req)))
          )
        val ctx = ctxAuth <+> ctxNoAuth

        org.http4s.server
          .ContextMiddleware[F, Context.OptionalAuth](ctx)
          .apply(Kleisli(req => inner(req.context)(req.req).withOptAuthCookie(req)))
      }

      extension (self: OptionT[F, Response[F]])
        def withAuthCookie(
            req: ContextRequest[F, Context.Authenticated]
        ): OptionT[F, Response[F]] =
          val baseUrl = ClientRequestInfo.getBaseUrl(cfg, req.req)
          val cookie = WebappstubAuth(req.context.token.asString).asCookie(baseUrl)
          self.map(_.addCookie(cookie))

        def withOptAuthCookie(
            req: ContextRequest[F, Context.OptionalAuth]
        ): OptionT[F, Response[F]] =
          req.context.token match
            case None => self
            case Some(token) =>
              val baseUrl = ClientRequestInfo.getBaseUrl(cfg, req.req)
              val cookie = WebappstubAuth(token.asString).asCookie(baseUrl)
              self.map(_.addCookie(cookie))
    }
