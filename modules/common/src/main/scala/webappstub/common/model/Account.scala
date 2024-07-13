package webappstub.common.model

import java.time.Instant

final case class Account(
    id: AccountId,
    state: AccountState,
    login: LoginName,
    password: Password,
    createdAt: Instant
)
