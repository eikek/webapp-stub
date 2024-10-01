package webappstub.server.routes.login

import cats.data.OptionT
import cats.data.Validated
import cats.effect.*
import cats.syntax.all.*

import webappstub.backend.LoginService
import webappstub.backend.auth.*
import webappstub.common.model.*
import webappstub.server.routes.login.LoginError.*

import org.http4s.Request
import org.http4s.Uri
import scribe.Scribe
import soidc.borer.given
import soidc.http4s.client.ByteEntityDecoder
import soidc.http4s.routes.AuthCodeFlow
import soidc.http4s.routes.GetToken
import soidc.http4s.routes.JwtAuth
import soidc.jwt.JoseHeader
import soidc.jwt.SimpleClaims

final private class LoginApi[F[_]: Sync](login: LoginService[F])
    extends ByteEntityDecoder:
  val rememberMeCookie = "webappstub_remember_me"
  private val rememberMeAuth = JwtAuth
    .builder[F, JoseHeader, SimpleClaims]
    .withGetToken(GetToken.cookie(rememberMeCookie))
    .withValidator(login.rememberMeValidator)
    .securedOrAnonymous

  def findRememberMe(req: Request[F]): F[Option[RememberMeToken]] =
    rememberMeAuth(req).subflatMap(_.getToken).value

  def doLogin(in: Model.UserPasswordForm): F[LoginValid[LoginResult]] =
    in.toModel match
      case e @ Validated.Invalid(_) => e.pure[F]
      case Validated.Valid(up)      => login.loginInternal(up).map(_.valid)

  def doRememberMeLogin(rkey: RememberMeToken) =
    login.loginRememberMe(rkey)

  def openIdRealm(
      baseUri: Uri,
      logger: Scribe[F],
      name: String
  ): OptionT[F, AuthCodeFlow[F, JoseHeader, SimpleClaims]] =
    val resumeSegment = "resume"
    OptionT(login.openIdRealms(baseUri, resumeSegment).map(_.get(name)))
      .semiflatMap(acf =>
        AuthCodeFlow(
          AuthCodeFlow.Config(baseUri, resumeSegment),
          acf,
          SoidcLogger(logger)
        )
      )
