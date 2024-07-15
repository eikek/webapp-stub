package webappstub.common.model

import java.time.Instant

final case class NewAccount(
    state: AccountState,
    login: LoginName,
    password: Password
):
  def withId(id: AccountId, created: Instant): Account =
    Account(id, state, login, password, created)

object NewAccount:
  def active(login: LoginName, pass: Password): NewAccount =
    NewAccount(AccountState.Active, login, pass)
