package webappstub.backend

import webappstub.backend.auth.AuthConfig
import webappstub.store.PostgresConfig

final case class BackendConfig(
    database: PostgresConfig,
    auth: AuthConfig
)
