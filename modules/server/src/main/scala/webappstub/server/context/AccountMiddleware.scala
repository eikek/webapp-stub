package webappstub.server.context

import cats.Monad
import cats.data.Kleisli
import cats.data.OptionT
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.server.context.AccountMiddleware.Config

import org.http4s.ContextRequest
import org.http4s.Response

final class AccountMiddleware[F[_]: Monad](cfg: Config[F]):

  def required(service: AccountRoutes[F]): JwtRoutes[F] =
    Kleisli(jwtReq => forToken(jwtReq, service))

  def optional(service: MaybeAccountRoutes[F]): MaybeJwtRoutes[F] =
    Kleisli { jwtReq =>
      jwtReq.context.toAuthenticated match
        case Some(c) =>
          val jwr: ContextRequest[F, Authenticated] = ContextRequest(c, jwtReq.req)
          forToken(jwr, service.local(_.widen))
        case None =>
          val nextReq = ContextRequest(Context.none, jwtReq.req)
          service(nextReq)
    }

  private def forToken(
      jwtReq: ContextRequest[F, Authenticated],
      service: AccountRoutes[F]
  ) =
    val token = jwtReq.context.token
    token.accountKey match
      case Some(key) =>
        val nextReq = OptionT(cfg.lookup(key)).map(a =>
          ContextRequest(Context.Account(a.id, token.claims), jwtReq.req)
        )
        OptionT(
          nextReq.fold(cfg.onNotFound(key).map(_.some))(service.run(_).value).flatten
        )
      case None => OptionT.none

object AccountMiddleware:
  final case class Config[F[_]](
      lookup: AccountKey => F[Option[Account]],
      onNotFound: AccountKey => F[Response[F]]
  )

  def builder[F[_]: Monad]: Builder[F] = new Builder[F](
    Config(
      lookup = _ => None.pure[F],
      onNotFound = _ => Response.notFound[F].pure[F]
    )
  )

  final case class Builder[F[_]: Monad](cfg: Config[F]) {
    lazy val value: AccountMiddleware[F] = new AccountMiddleware[F](cfg)

    def required(service: AccountRoutes[F]): JwtRoutes[F] =
      value.required(service)

    def optional(service: MaybeAccountRoutes[F]): MaybeJwtRoutes[F] =
      value.optional(service)

    def withLookup(lookup: AccountKey => F[Option[Account]]): Builder[F] =
      copy(cfg = cfg.copy(lookup = lookup))

    def withOnNotFound(f: AccountKey => F[Response[F]]): Builder[F] =
      copy(cfg = cfg.copy(onNotFound = f))
  }
