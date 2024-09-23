package webappstub.backend.auth

import scala.concurrent.duration.*

import soidc.jwt.JWK

case class AuthConfig(
    serverSecret: Option[JWK],
    sessionValid: FiniteDuration,
    authType: AuthConfig.AuthenticationType,
    rememberMeValid: FiniteDuration
):
  def rememberMeEnabled: Boolean = rememberMeValid > Duration.Zero

object AuthConfig {

  final case class Proxy(
      enabled: Boolean,
      userHeader: String,
      emailHeader: Option[String]
  ) {
    def disabled = !enabled
  }

  enum AuthenticationType:
    case Fixed
    case Internal
}
