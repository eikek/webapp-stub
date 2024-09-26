package webappstub.server

import webappstub.backend.auth.*
import webappstub.common.model.*

import soidc.http4s.routes.JwtContext
import soidc.jwt.*

package object context {
  type Authenticated = JwtContext.Authenticated[JoseHeader, SimpleClaims]
  type MaybeAuthenticated = JwtContext.MaybeAuthenticated[JoseHeader, SimpleClaims]
  type Context = JwtContext[JoseHeader, SimpleClaims]

  extension (self: Authenticated)
    def account: Option[Either[AccountId, ExternalAccountId]] = self.token.accountId
}
