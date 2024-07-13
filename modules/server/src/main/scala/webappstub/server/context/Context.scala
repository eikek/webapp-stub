package webappstub.server.context

import webappstub.common.model.AccountId

trait Context:
  def settings: Settings

object Context:

  final case class Authenticated(account: AccountId, settings: Settings) extends Context:
    def toOptional: OptionalAuth = OptionalAuth(Some(account), settings)

  final case class OptionalAuth(account: Option[AccountId], settings: Settings)
      extends Context

  final case class Alias(aliasId: String, settings: Settings) extends Context
