package webappstub.server.routes.login

import cats.Monad
import cats.data.Validated
import cats.syntax.all.*

import webappstub.backend.LoginService
import webappstub.backend.auth.LoginResult
import webappstub.backend.auth.RememberMeToken
import webappstub.server.routes.login.LoginError.*

import org.http4s.Request
import soidc.borer.given
import soidc.http4s.routes.GetToken
import soidc.http4s.routes.JwtAuth
import soidc.jwt.JoseHeader
import soidc.jwt.SimpleClaims

final class LoginApi[F[_]: Monad](login: LoginService[F]):
  val rememberMeCookie = "webappstub_remember_me"
  private val rememberMeAuth = JwtAuth
    .builder[F, JoseHeader, SimpleClaims]
    .withGetToken(GetToken.cookie(rememberMeCookie))
    .withValidator(login.rememberMeValidator)
    .optional

  def findRememberMe(req: Request[F]): F[Option[RememberMeToken]] =
    rememberMeAuth(req).subflatMap(_.token).value

  def doLogin(in: Model.UserPasswordForm): F[LoginValid[LoginResult]] =
    in.toModel match
      case e @ Validated.Invalid(_) => e.pure[F]
      case Validated.Valid(up)      => login.loginUserPass(up).map(_.valid)

  def doRememberMeLogin(rkey: RememberMeToken) =
    login.loginRememberMe(rkey)
