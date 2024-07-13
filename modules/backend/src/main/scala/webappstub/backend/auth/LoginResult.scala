package webappstub.backend.auth

enum LoginResult:
  case Success(token: AuthToken)
  case InvalidAuth
  case InvalidTime

  def fold[A](whenFail: => A, whenSuccess: AuthToken => A): A =
    this match
      case Success(token) => whenSuccess(token)
      case _              => whenFail
