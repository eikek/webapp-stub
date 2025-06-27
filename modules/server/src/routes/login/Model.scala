package webappstub.server.routes.login

import cats.syntax.all.*

import webappstub.backend.auth.UserPass
import webappstub.common.model.*
import webappstub.server.common.ParamDecoders
import webappstub.server.routes.login.LoginError.*

import htmx4s.http4s.util.ValidationDsl.*
import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*
import org.http4s.QueryParamDecoder
import soidc.jwt.JWS

object Model extends ParamDecoders:
  final case class UserPasswordForm(user: String, password: String, rememberMe: Boolean):
    def toModel: LoginValid[UserPass] = {
      val login = LoginName.fromString(user).toValidatedNel.keyed(Key.LoginName)
      login.map(l => UserPass(l, Password(password), rememberMe))
    }

  object UserPasswordForm:
    given FormDataDecoder[UserPasswordForm] =
      (
        field[String]("username"),
        field[String]("password"),
        fieldOptional[Boolean]("rememberMe").map(_.getOrElse(false))
      )
        .mapN(UserPasswordForm.apply)

  final case class SignupForm(
      user: String = "",
      inviteKey: Option[String] = None
  ):
    def toModel(
        id: ExternalAccountId,
        refreshToken: Option[JWS]
    ): LoginValid[SignupRequest] = {
      val login = LoginName.fromString(user).toValidatedNel.keyed(Key.LoginName)
      val ik = inviteKey match
        case None => None.valid[Key, String]
        case Some(k) =>
          InviteKey.fromString(k).toValidatedNel.keyed(Key.Invite).map(_.some)

      (login, Some(id).valid, refreshToken.valid, ik).mapN(SignupRequest.external)
    }

  object SignupForm:
    given FormDataDecoder[SignupForm] =
      (field[String]("username"), fieldOptional[String]("inviteKey"))
        .mapN(SignupForm.apply)
