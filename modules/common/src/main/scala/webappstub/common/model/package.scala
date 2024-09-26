package webappstub.common

import soidc.jwt.*

package object model {

  type AuthToken = JWSDecoded[JoseHeader, SimpleClaims]
  type RememberMeToken = JWSDecoded[JoseHeader, SimpleClaims]

  extension (self: AuthToken)
    def provider: Option[Provider] =
      self.claims.issuer match
        case Some(v) if v.value == Provider.internal.value => Some(Provider.internal)
        case Some(v) => Some(Provider(v.value))
        case None => None

    def accountId: Option[Either[AccountId, ExternalAccountId]] =
      provider match
        case Some(Provider.internal) =>
          self.claims.subject.flatMap(_.value.toLongOption).map(n => Left(AccountId(n)))
        case Some(p) =>
          ExternalAccountId.fromToken(self).map(Right(_))
        case None => None

    def rememberMeKey: Option[RememberMeKey] =
      provider match
        case Some(Provider.internal) =>
          self.claims.subject.map(_.value).flatMap(s => RememberMeKey.fromString(s).toOption)
        case _ => None

}
