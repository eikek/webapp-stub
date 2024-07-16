package webappstub.server.routes.settings

import webappstub.server.data.UiLanguage
import webappstub.server.data.UiTheme
import webappstub.server.routes.{Components, Styles}

import htmx4s.scalatags.Bundle.*
import io.bullet.borer.Json
import scalatags.Text.TypedTag

object Views extends Components:

  def themeButton(selected: UiTheme): TypedTag[String] =
    a(
      cls := s"py-2 px-4 inline-flex items-center rounded-lg ${Styles.basicBtnHover} ${Styles.borderBtn} ${Styles.focusRingBtn} ${Styles.basicBtnBg} text-xl",
      attr.title := s"Switch to ${UiTheme.cycle(selected).name}",
      i(cls := selected.fold("fa fa-moon", "fa fa-sun")),
      attr.hxPost := "/app/settings/cycle-theme"
    )

  def languageDropdown(selected: UiLanguage, open: Boolean): TypedTag[String] =
    val hidden = if (open) "" else "hidden"
    val qopen = if (open) "" else "open"
    div(
      cls := s"relative dark:bg-stone-800 bg-white",
      attr.id := "language-dropdown",
      a(
        cls := s"py-2 pl-2 pr-5 rounded-lg inline-flex items-center ${Styles.borderBtn} ${Styles.focusRingBtn} ${Styles.basicBtnBg} ${Styles.basicBtnHover} text-sm",
        attr.hxGet := s"/app/settings/language?$qopen",
        attr.hxTarget := "#language-dropdown",
        span(cls := "mr-2", languageFlag(selected)),
        span(cls := "mr-2", selected.label),
        div(
          cls := "absolute right-2",
          i(cls := "fa fa-angle-down")
        )
      ),
      div(
        cls := s"flex flex-col space-y-1 absolute top-10 left-0 ${Styles.borderBtn} $hidden",
        UiLanguage.all.map { lang =>
          a(
            attr.hxPost := "/app/settings/language",
            attr.hxVals := Json.encode(Map("language" -> lang.iso3)).toUtf8String,
            cls := s"px-1 py-1 flex ${Styles.basicBtnHover}",
            span(cls := "mr-2", languageFlag(lang)),
            span(cls := "mr-2", lang.label)
          )
        }
      )
    )
