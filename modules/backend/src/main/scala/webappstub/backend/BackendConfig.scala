package webappstub.backend

import webappstub.store.PostgresConfig

final case class BackendConfig(
  database: PostgresConfig
)
