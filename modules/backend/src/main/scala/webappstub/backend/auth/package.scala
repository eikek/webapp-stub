package webappstub.backend

import webappstub.common.model.AccountId
import webappstub.common.model.RememberMeKey

import soidc.core.LocalFlow
import soidc.core.Realm
import soidc.jwt.*

package object auth {

  type WebappstubRealm[F[_]] = Realm[F, JoseHeader, SimpleClaims]

  type AuthToken = JWSDecoded[JoseHeader, SimpleClaims]
  type RememberMeToken = JWSDecoded[JoseHeader, SimpleClaims]

  extension [F[_]](self: LocalFlow[F, JoseHeader, SimpleClaims])
    def makeToken(accountId: AccountId): F[AuthToken] =
      self.createToken(
        JoseHeader.jwt,
        SimpleClaims.empty.withSubject(StringOrUri(accountId.value.toString()))
      )

  extension (self: AuthToken)
    def accountId: AccountId = AccountId(
      self.claims.subject.map(_.value.toLong).getOrElse(Long.MinValue)
    )
    def rememberMeKey: Option[RememberMeKey] =
      self.claims.subject.map(_.value).flatMap(s => RememberMeKey.fromString(s).toOption)
}
