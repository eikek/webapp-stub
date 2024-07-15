package webappstub.backend

import webappstub.backend.auth.AuthConfig
import webappstub.backend.signup.SignupConfig
import webappstub.store.PostgresConfig

final case class BackendConfig(
    database: PostgresConfig,
    auth: AuthConfig,
    signup: SignupConfig
)
