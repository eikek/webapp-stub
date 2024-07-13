package webappstub.backend.auth

import scala.concurrent.duration.Duration

import scodec.bits.ByteVector

case class AuthConfig(
    serverSecret: ByteVector,
    sessionValid: Duration,
    authType: AuthConfig.AuthenticationType
)

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
