package webappstub.common.model

import soidc.jwt.JWS

final case class SignupRequest(
    login: LoginName,
    password: Password,
    externalId: Option[ExternalAccountId],
    refreshToken: Option[JWS],
    inviteKey: Option[InviteKey]
)

object SignupRequest:

  def internal(
      login: LoginName,
      password: Password,
      inviteKey: Option[InviteKey]
  ): SignupRequest =
    SignupRequest(login, password, None, None, inviteKey)

  def external(
      login: LoginName,
      externalId: Option[ExternalAccountId],
      refreshToken: Option[JWS],
      inviteKey: Option[InviteKey]
  ): SignupRequest =
    SignupRequest(login, Password(""), externalId, refreshToken, inviteKey)
