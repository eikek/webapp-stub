package webappstub.server.routes.login

import webappstub.server.context.Settings
import webappstub.server.routes.settings.Views.{languageDropdown, themeButton}
import webappstub.server.routes.{Components, Styles, UiConfig}

import htmx4s.scalatags.Bundle.*
import scalatags.Text.TypedTag

object View extends Components:
  def view(cfg: UiConfig, ctx: Settings): TypedTag[String] =
    val texts = I18n(ctx.language)
    div(
      attr.id := "login-form",
      cls := s"md:h-screen-12 flex flex-col items-center justify-center items-center w-full ${Styles.content}",
      attr.hxExt := "response-targets",
      div(
        cls := s"flex flex-col px-2 sm:px-4 py-4 rounded-md min-w-full md:min-w-0 md:w-96 ${Styles.boxMd}",
        div(
          cls := "self-center",
          h2(cls := "text-2xl font-bold mx-auto", cfg.name)
        ),
        form(
          cls := "",
          attr.method := "POST",
          hxTargetError := "#error-message",
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
                attr.placeholder := texts.loginPlaceholder
              )
            )
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
                attr.autocomplete := false,
                cls := s"pl-10 pr-4 py-2 rounded-lg ${Styles.textInput}",
                attr.placeholder := texts.passwordPlaceholder
              )
            )
          ),
          Option.when(cfg.rememberMeEnabled) {
            div(
              cls := "mx-2 flex",
              input(
                cls := "mr-2",
                attr.`type` := "checkbox",
                attr.name := "rememberMe",
                "Remember me"
              )
            )
          },
          div(
            cls := "flex flex-col my-3",
            button(
              attr.`type` := "submit",
              cls := Styles.primaryButton,
              texts.loginButton
            )
          )
        ),
        div(attr.id := "error-message")
      ),
      div(
        cls := "mt-4 flex flex-row items-center space-x-4 py-2",
        languageDropdown(ctx.language, false),
        themeButton(ctx.theme)
      ),
      div(
        cls := "text-sm",
        attr.hxBoost := true,
        "No account?",
        a(
          cls := s"ml-2 ${Styles.link}",
          i(cls := "fa fa-user-plus mr-1"),
          "Sign up",
          attr.href := "/app/signup"
        )
      )
    )

  def loginFailed(error: String): TypedTag[String] =
    div(
      cls := s"mt-2 ${Styles.errorMessage}",
      error
    )
