package webappstub.server.routes

import webappstub.server.data.UiLanguage

import htmx4s.http4s.util.ValidationErrors
import htmx4s.scalatags.Bundle.*
import scalatags.Text.TypedTag

trait Components:
  val hxTarget400 = attr("hx-target-400")
  val hxTarget422 = attr("hx-target-422")
  val hxTargetError = attr("hx-target-error")

  def languageFlag(lang: UiLanguage): TypedTag[String] =
    i(cls := s"fi fi-${lang.iso2}")

  def fieldErrors[K](key: K, errors: Option[ValidationErrors[K, String]]) =
    errors.flatMap(_.find(key)).map { msgs =>
      div(
        cls := s"mt-2 ${Styles.errorMessage}",
        ul(msgs.toList.map(li(_)))
      )
    }

object Components extends Components
