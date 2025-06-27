package webappstub.backend.signup

import webappstub.common.model.Account

enum SignupResult:
  case Success(account: Account)
  case LoginExists
  case SignupClosed
  case InvalidKey

  def toEither: Either[SignupResult, SignupResult] = this match
    case a: Success => Right(a)
    case b          => Left(b)
