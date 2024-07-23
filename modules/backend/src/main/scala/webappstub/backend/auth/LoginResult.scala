package webappstub.backend.auth

enum LoginResult:
  case Success(token: AuthToken, rememberMe: Option[RememberMeToken])
  case InvalidAuth

  def fold[A](whenFail: => A, whenSuccess: (AuthToken, Option[RememberMeToken]) => A): A =
    this match
      case Success(token, rm) => whenSuccess(token, rm)
      case _                  => whenFail
