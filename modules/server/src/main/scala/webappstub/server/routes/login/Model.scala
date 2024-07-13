package webappstub.server.routes.login

import cats.syntax.all.*

import webappstub.backend.auth.UserPass
import webappstub.common.model.*

import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*
import org.http4s.ParseFailure
import org.http4s.QueryParamDecoder

object Model:
  given QueryParamDecoder[Password] =
    QueryParamDecoder[String].map(Password.apply)

  given QueryParamDecoder[LoginName] =
    QueryParamDecoder[String].emap(s =>
      LoginName.fromString(s).leftMap(err => ParseFailure(err, err))
    )

  final case class UserPasswordForm(user: LoginName, password: Password):
    def toModel: UserPass = UserPass(user, password)

  object UserPasswordForm:
    given FormDataDecoder[UserPasswordForm] =
      (field[LoginName]("username"), field[Password]("password"))
        .mapN(UserPasswordForm.apply)
