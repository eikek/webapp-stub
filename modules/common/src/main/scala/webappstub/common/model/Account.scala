package webappstub.common.model

import java.time.Instant

import soidc.jwt.JWS

final case class Account(
    id: AccountId,
    state: AccountState,
    login: LoginName,
    password: Password,
    externalId: Option[ExternalAccountId],
    refreshToken: Option[JWS],
    createdAt: Instant
)
