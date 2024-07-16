package webappstub.server.routes.signup

import cats.data.Validated
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.server.routes.signup.SignupError.Key

import htmx4s.http4s.util.ErrorMessage
import htmx4s.http4s.util.ValidationDsl.*
import htmx4s.http4s.util.ValidationErrors
import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*
import org.http4s.QueryParamDecoder

object Model:
  final case class CreateInviteForm(
      secret: String = ""
  ):
    def toModel: SignupValid[Password] =
      secret
        .asNonEmpty(Key.ServerSecret, "The server secret is required")
        .map(Password(_))

  object CreateInviteForm:
    given FormDataDecoder[CreateInviteForm] =
      field[String]("secret").map(CreateInviteForm.apply)

  final case class CreateInvitePage(
    form: CreateInviteForm = CreateInviteForm(),
    errors: Option[Errors] = None,
    key: Option[InviteKey] = None
  )

  final case class SignupForm(
      user: String = "",
      password: String = "",
      passwordConfirm: String = "",
      inviteKey: Option[String] = None
  ):
    def toModel: SignupValid[SignupRequest] = {
      val login = LoginName.fromString(user).toValidatedNel.keyed(Key.LoginName)
      val pw = (password.asNonEmpty(Key.Password, "Password is required") |+|
        Validated.cond(
          password == passwordConfirm,
          password,
          ValidationErrors.of("Passwords don't match".keyed(Key.PasswordConfirm))
        )).as(Password(password))

      val ik = inviteKey match
        case None => None.valid[Key, String]
        case Some(k) =>
          InviteKey.fromString(k).toValidatedNel.keyed(Key.Invite).map(_.some)

      (login, pw, ik).mapN(SignupRequest.apply)
    }

  object SignupForm:
    given FormDataDecoder[SignupForm] =
      (
        field[String]("username"),
        field[String]("password"),
        field[String]("passwordConfirm"),
        fieldOptional[String]("inviteKey")
      )
        .mapN(SignupForm.apply)
