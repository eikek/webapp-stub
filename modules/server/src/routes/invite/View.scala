package webappstub.server.routes.invite

import webappstub.server.context.Settings
import webappstub.server.routes.invite.InviteError.Key
import webappstub.server.routes.signup.I18n
import webappstub.server.routes.{Components, Styles}

import htmx4s.scalatags.Bundle.*

object View extends Components:
  object Id:
    val createInviteForm = "create-invite-form"

  def view(ctx: Settings, model: Model.CreateInvitePage) =
    val texts = I18n(ctx.language)
    val target = s"#${Id.createInviteForm}"
    div(
      attr.id := Id.createInviteForm,
      cls := s"flex flex-col w-full mt-2 ${Styles.content}",
      attr.hxExt := "response-targets",
      h2(cls := "text-2xl font-bold", s"Create Invite Keys"),
      form(
        cls := "",
        attr.method := "POST",
        attr.hxPost := "",
        attr.hxTarget := target,
        hxTargetError := target,
        attr.autocomplete := false,
        div(
          cls := "flex flex-col mt-6",
          label(
            attr.`for` := "secret",
            cls := Styles.inputLabel,
            "Server Secret"
          ),
          div(
            cls := "relative",
            div(
              cls := Styles.inputIcon,
              i(cls := "fa fa-lock-open")
            ),
            input(
              attr.`type` := "text",
              attr.name := "secret",
              attr.autocomplete := false,
              cls := s"pl-10 pr-4 py-2 rounded-lg ${Styles.textInput}",
              attr.value := model.form.secret
            )
          ),
          fieldErrors(Key.ServerSecret, model.errors)
        ),
        div(
          cls := "flex flex-col my-3",
          button(
            attr.`type` := "submit",
            cls := Styles.primaryButton,
            texts.submitButton
          )
        )
      ),
      model.key.map { k =>
        div(
          cls := "flex flex-col",
          div(
            cls := s"mx-2 my-2 ${Styles.successMessage}",
            k.value
          ),
          div(
            cls := "my-2 mx-2",
            "Give this key to someone so they can register. Each key can only be used once."
          )
        )
      }
    )
