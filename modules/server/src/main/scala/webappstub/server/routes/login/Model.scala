package webappstub.server.routes.login

import cats.syntax.all.*

import webappstub.backend.auth.UserPass
import webappstub.common.model.*
import htmx4s.http4s.util.ValidationDsl.*
import webappstub.server.routes.login.LoginError.*

import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*
import org.http4s.QueryParamDecoder

object Model:
  final case class UserPasswordForm(user: String, password: String):
    def toModel: LoginValid[UserPass] = {
      val login = LoginName.fromString(user).toValidatedNel.keyed(Key.LoginName)
      login.map(l => UserPass(l, Password(password)))
    }

  object UserPasswordForm:
    given FormDataDecoder[UserPasswordForm] =
      (field[String]("username"), field[String]("password"))
        .mapN(UserPasswordForm.apply)
