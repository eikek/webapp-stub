package webappstub.server.routes

import webappstub.server.data.UiLanguage

import htmx4s.scalatags.Bundle.*
import scalatags.Text.TypedTag

object Components:

  def languageFlag(lang: UiLanguage): TypedTag[String] =
    i(cls := s"fi fi-${lang.iso2}")
