package webappstub.server.routes.signup

import cats.syntax.all.*

import webappstub.backend.signup.{SignupMode, SignupResult}
import webappstub.server.context.Settings
import webappstub.server.routes.settings.Views.{languageDropdown, themeButton}
import webappstub.server.routes.signup.SignupError.Key
import webappstub.server.routes.{Components, Styles, UiConfig}

import htmx4s.scalatags.Bundle.*
import scalatags.Text.TypedTag

object View extends Components:
  object Id:
    val signupForm = "signup-form"

  def view(
      cfg: UiConfig,
      ctx: Settings,
      mode: SignupMode,
      form: Model.SignupForm,
      errors: Option[Errors]
  ): TypedTag[String] =
    div(
      attr.id := Id.signupForm,
      cls := s"h-dvh flex flex-col items-center justify-center items-center w-full ${Styles.content}",
      attr.hxExt := "response-targets",
      div(
        cls := s"flex flex-col px-2 sm:px-4 py-4 rounded-md min-w-full md:min-w-0 md:w-96 ${Styles.boxMd}",
        div(
          cls := "self-center",
          h2(cls := "text-2xl font-bold mx-auto", s"Signup - ${cfg.name}")
        ),
        signupForm(form, ctx, mode, errors)
      ),
      div(
        cls := "mt-4 flex flex-row items-center space-x-4 py-2",
        languageDropdown(ctx.language, false),
        themeButton(ctx.theme)
      ),
      div(
        cls := "text-sm",
        attr.hxBoost := true,
        "Account already?",
        a(
          cls := s"ml-2 ${Styles.link}",
          i(cls := "fa fa-right-to-bracket mr-1"),
          "Login",
          attr.href := "/app/login"
        )
      )
    )

  def signupForm(
      data: Model.SignupForm,
      ctx: Settings,
      mode: SignupMode,
      errors: Option[Errors]
  ) =
    val texts = I18n(ctx.language)
    form(
      cls := "",
      attr.method := "POST",
      hxTarget400 := "this",
      hxTarget422 := "#error-message",
      attr.hxPost := "",
      attr.autocomplete := false,
      div(
        cls := "flex flex-col mt-6",
        label(attr.`for` := "username", cls := Styles.inputLabel, texts.username),
        div(
          cls := "relative",
          div(
            cls := Styles.inputIcon,
            i(cls := "fa fa-user")
          ),
          input(
            attr.`type` := "text",
            attr.name := "username",
            attr.autocomplete := false,
            cls := s"pl-10 pr-4 py-2 rounded-lg ${Styles.textInput}",
            attr.placeholder := texts.loginPlaceholder,
            attr.value := data.user
          )
        ),
        fieldErrors(Key.LoginName, errors)
      ),
      div(
        cls := "flex flex-col my-3",
        label(attr.`for` := "password", cls := Styles.inputLabel, texts.password),
        div(
          cls := "relative",
          div(cls := Styles.inputIcon, i(cls := "fa fa-lock")),
          input(
            attr.`type` := "password",
            attr.name := "password",
            attr.autocomplete := true,
            cls := s"pl-10 pr-4 py-2 rounded-lg ${Styles.textInput}",
            attr.placeholder := texts.passwordPlaceholder,
            attr.value := data.password
          )
        ),
        fieldErrors(Key.Password, errors)
      ),
      div(
        cls := "flex flex-col my-3",
        label(
          attr.`for` := "passwordConfirm",
          cls := Styles.inputLabel,
          texts.passwordConfirm
        ),
        div(
          cls := "relative",
          div(cls := Styles.inputIcon, i(cls := "fa fa-lock")),
          input(
            attr.`type` := "password",
            attr.name := "passwordConfirm",
            attr.autocomplete := true,
            cls := s"pl-10 pr-4 py-2 rounded-lg ${Styles.textInput}",
            attr.placeholder := texts.passwordConfirmPlaceholder,
            attr.value := data.passwordConfirm
          )
        ),
        fieldErrors(Key.PasswordConfirm, errors)
      ),
      Option.when(mode.isInvite) {
        div(
          cls := "flex flex-col my-3",
          label(
            attr.`for` := "inviteKey",
            cls := Styles.inputLabel,
            texts.inviteKey
          ),
          div(
            cls := "relative",
            div(cls := Styles.inputIcon, i(cls := "fa fa-lock")),
            input(
              attr.`type` := "text",
              attr.name := "inviteKey",
              attr.autocomplete := true,
              cls := s"pl-10 pr-4 py-2 rounded-lg ${Styles.textInput}",
              attr.placeholder := texts.inviteKeyPlaceholder,
              attr.value := data.inviteKey.orEmpty
            )
          ),
          fieldErrors(Key.Invite, errors)
        )
      },
      div(
        cls := "mt-1 mb-2",
        fieldErrors(Key.Default, errors)
      ),
      div(attr.id := "error-message"),
      div(
        cls := "flex flex-col my-3",
        button(
          attr.`type` := "submit",
          cls := Styles.primaryButton,
          texts.submitButton
        )
      )
    )

  def signupResult(result: SignupResult) =
    div(
      cls := s"mt-2 ${Styles.errorMessage}",
      result match
        case SignupResult.Success(_)   => ""
        case SignupResult.InvalidKey   => "Invalid invitation key"
        case SignupResult.SignupClosed => "Signup is closed"
        case SignupResult.LoginExists  => "Login is already taken"
    )
