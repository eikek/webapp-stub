package webappstub.common.model

import java.time.Instant

import soidc.jwt.JWS

final case class NewAccount(
    state: AccountState,
    login: LoginName,
    password: Password,
    externalId: Option[ExternalAccountId],
    refreshToken: Option[JWS]
):
  def withId(id: AccountId, created: Instant): Account =
    Account(id, state, login, password, externalId, refreshToken, created)

object NewAccount:
  def internalActive(login: LoginName, pass: Password): NewAccount =
    NewAccount(AccountState.Active, login, pass, None, None)

  def externalActive(
      login: LoginName,
      externalId: ExternalAccountId,
      refreshToken: Option[JWS]
  ): NewAccount =
    NewAccount(AccountState.Active, login, Password(""), Some(externalId), refreshToken)
