package webappstub.server

import cats.data.{Kleisli, OptionT}

import org.http4s.*
import soidc.http4s.routes.*
import soidc.jwt.*

package object context {
  type Authenticated = JwtContext.Authenticated[JoseHeader, SimpleClaims]
  type MaybeAuthenticated = JwtContext[JoseHeader, SimpleClaims]
  type JwtRoutes[F[_]] = JwtAuthRoutes[F, JoseHeader, SimpleClaims]
  type MaybeJwtRoutes[F[_]] = JwtMaybeAuthRoutes[F, JoseHeader, SimpleClaims]

  type AccountRoutes[F[_]] =
    Kleisli[OptionT[F, *], ContextRequest[F, Context.Account], Response[F]]
  type MaybeAccountRoutes[F[_]] =
    Kleisli[OptionT[F, *], ContextRequest[F, Context], Response[F]]
}
