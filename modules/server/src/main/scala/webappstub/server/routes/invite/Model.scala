package webappstub.server.routes.invite

import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.server.routes.invite.InviteError.Key

import htmx4s.http4s.util.ValidationDsl.*
import org.http4s.FormDataDecoder
import org.http4s.FormDataDecoder.*
import org.http4s.QueryParamDecoder

object Model:
  final case class CreateInviteForm(
      secret: String = ""
  ):
    def toModel: InviteValid[Password] =
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
