package webappstub.server.common

import webappstub.server.data.UiLanguage

import org.http4s.*
import org.typelevel.ci.CIString

final case class WebappstubLang(lang: UiLanguage):
  def asCookie(baseUrl: Uri): ResponseCookie =
    HeaderReader.asCookie(baseUrl)(WebappstubLang.cookieName, lang.iso3)

object WebappstubLang:
  val name: CIString = CIString("Webappstub-Lang")
  private val cookieName = "webappstub_lang"

  def parse(str: String): ParseResult[WebappstubLang] =
    UiLanguage
      .fromString(str)
      .left
      .map(err => ParseFailure(err, err))
      .map(WebappstubLang.apply)

  given Header[WebappstubLang, Header.Single] =
    Header.create(name, _.lang.iso3, parse)

  def fromRequest[F[_]](req: Request[F]) =
    HeaderReader.from[F, WebappstubLang](req, Some(cookieName))
