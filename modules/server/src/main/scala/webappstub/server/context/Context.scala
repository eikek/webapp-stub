package webappstub.server.context

import webappstub.common.model.*
import soidc.jwt.SimpleClaims

trait Context:
  def isAuthenticated: Boolean
  def widen: Context = this

object Context:
  val none: Context = NoAccount
  def apply(id: AccountId, claims: SimpleClaims): Context = Account(id, claims)


  final case class Account(
      id: AccountId,
      claims: SimpleClaims
  ) extends Context:
    val isAuthenticated = true

  case object NoAccount extends Context {
    val isAuthenticated = false
  }
