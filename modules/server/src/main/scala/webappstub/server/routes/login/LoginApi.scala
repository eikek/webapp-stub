package webappstub.server.routes.login

import cats.Applicative
import cats.data.Validated
import cats.syntax.all.*

import webappstub.backend.LoginService
import webappstub.backend.auth.LoginResult
import webappstub.server.routes.login.LoginError.*

final class LoginApi[F[_]: Applicative](login: LoginService[F]):

  def doLogin(in: Model.UserPasswordForm): F[LoginValid[LoginResult]] =
    in.toModel match
      case e @ Validated.Invalid(_) => e.pure[F]
      case Validated.Valid(up)      => login.loginUserPass(up).map(_.valid)
