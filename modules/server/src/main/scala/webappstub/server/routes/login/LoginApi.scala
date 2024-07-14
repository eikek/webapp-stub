package webappstub.server.routes.login

import cats.syntax.all.*
import LoginError.*
import webappstub.backend.auth.LoginResult
import webappstub.backend.LoginService
import cats.data.Validated
import cats.Applicative
import webappstub.server.common.WebappstubAuth

final class LoginApi[F[_]: Applicative](login: LoginService[F]):

  def doLogin(in: Model.UserPasswordForm): F[LoginValid[LoginResult]] =
    in.toModel match
      case e@ Validated.Invalid(_) => e.pure[F]
      case Validated.Valid(up) => login.loginUserPass(up).map(_.valid)

  def refreshToken(token: WebappstubAuth): F[LoginResult] =
    login.loginSession(token.token)
