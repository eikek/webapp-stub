package webappstub.common.model

final case class ExternalAccountId(provider: Provider, id: String)

object ExternalAccountId:

  def fromToken(token: AuthToken): Option[ExternalAccountId] =
    for
      iss <- token.claims.issuer
      sub <- token.claims.subject
    yield ExternalAccountId(Provider(iss.value), sub.value)
