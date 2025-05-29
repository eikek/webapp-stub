package webappstub.common

import soidc.jwt.*

package object model {

  type AuthToken = JWSDecoded[JoseHeader, SimpleClaims]
  type RefreshToken = JWSDecoded[JoseHeader, SimpleClaims]
  type IdToken = JWSDecoded[JoseHeader, SimpleClaims]
  type RememberMeToken = JWSDecoded[JoseHeader, SimpleClaims]

  extension (self: AuthToken)
    def provider: Option[Provider] =
      self.claims.issuer match
        case Some(Provider.internal) => Some(Provider.internal)
        case Some(v)                 => Some(Provider(v.value))
        case None                    => None

    def accountKey: Option[AccountKey] =
      provider match
        case Some(Provider.internal) =>
          self.claims.subject
            .flatMap(_.value.toLongOption)
            .map(n => AccountKey.Internal(AccountId(n)))
        case Some(_) =>
          ExternalAccountId.fromToken(self).map(AccountKey.External(_))
        case None => None

    def rememberMeKey: Option[RememberMeKey] =
      provider match
        case Some(Provider.internal) =>
          self.claims.subject
            .map(_.value)
            .flatMap(s => RememberMeKey.fromString(s).toOption)
        case _ => None
}
