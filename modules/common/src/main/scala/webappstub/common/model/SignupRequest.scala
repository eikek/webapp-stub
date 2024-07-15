package webappstub.common.model

final case class SignupRequest(
    login: LoginName,
    password: Password,
    inviteKey: Option[InviteKey]
)
