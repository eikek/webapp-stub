package webappstub.server.context

import cats.Monad
import cats.data.Kleisli
import cats.data.OptionT
import cats.syntax.all.*

import webappstub.backend.Backend
import webappstub.backend.auth.LoginResult
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
      login: String => F[LoginResult]
  ): GetContext[F, Context.Authenticated] =
    Kleisli { (req: Request[F]) =>
      OptionT(
        WebappstubAuth
          .fromRequest(req)
          .map(_.token)
          .traverse(login)
          .map {
            case Some(LoginResult.Success(token)) =>
              Some(Context.Authenticated(token.account, Settings.fromRequest(req)))

            case _ =>
              None
          }
      )
    }

  def forBackend[F[_]: Monad](backend: Backend[F]) =
    apply(backend.login.loginSession)

  def apply[F[_]: Monad](login: String => F[LoginResult]): ContextMiddleware[F] =
    new ContextMiddleware[F] {
      def secured(
          inner: Context.Authenticated => HttpRoutes[F],
          onFailure: Request[F] => F[Response[F]]
      ): HttpRoutes[F] =
        AuthMiddleware
          .noSpider(getContext(login), onFailure)
          .apply(Kleisli(req => inner(req.context)(req.req)))

      def securedOrRedirect(
          inner: Context.Authenticated => HttpRoutes[F],
          uri: Uri
      ): HttpRoutes[F] =
        secured(
          inner,
          _ =>
            Response(status = Status.SeeOther, headers = Headers(Location(uri))).pure[F]
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
          .apply(Kleisli(req => inner(req.context)(req.req)))
      }
    }
