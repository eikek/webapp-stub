package webappstub.backend.auth

import scala.concurrent.duration.*

import soidc.core.model.*
import soidc.jwt.{JWK, Uri}

case class AuthConfig(
    internal: AuthConfig.Internal,
    openId: Map[String, AuthConfig.OpenId]
):
  def rememberMeEnabled: Boolean =
    internal.enabled && internal.rememberMeValid > Duration.Zero

  def authDisabled: Boolean = !internal.enabled && openId.isEmpty

object AuthConfig {
  final case class Internal(
      enabled: Boolean,
      serverSecret: Option[JWK],
      sessionValid: FiniteDuration,
      rememberMeValid: FiniteDuration
  )
  final case class OpenId(
      providerUrl: Uri,
      clientId: ClientId,
      clientSecret: ClientSecret,
      scope: Option[ScopeList]
  )
}
