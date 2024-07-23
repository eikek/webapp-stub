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
