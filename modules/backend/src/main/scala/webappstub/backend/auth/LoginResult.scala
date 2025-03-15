package webappstub.backend.auth

import webappstub.common.model.*

enum LoginResult:
  case Success(token: AuthToken, rememberMe: Option[RememberMeToken])
  case AccountMissing
  case InvalidAuth

  def fold[A](whenFail: => A, whenSuccess: (AuthToken, Option[RememberMeToken]) => A): A =
    this match
      case Success(token, rm) => whenSuccess(token, rm)
      case _                  => whenFail
