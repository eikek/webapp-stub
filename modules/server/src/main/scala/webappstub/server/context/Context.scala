package webappstub.server.context

import webappstub.backend.auth.AuthToken
import webappstub.common.model.AccountId

trait Context:
  def settings: Settings
  def toOptional: Context.OptionalAuth
  def isAuthenticated: Boolean = toOptional.token.isDefined

object Context:

  final case class Authenticated(token: AuthToken, settings: Settings) extends Context:
    def toOptional: OptionalAuth = OptionalAuth(Some(token), settings)
    def account: AccountId = token.value

  final case class OptionalAuth(token: Option[AuthToken], settings: Settings)
      extends Context:
    def toOptional: OptionalAuth = this
