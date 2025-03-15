package webappstub.server.common

import webappstub.server.data.UiTheme

import org.http4s.*
import org.typelevel.ci.CIString

final case class WebappstubTheme(theme: UiTheme):
  def asCookie(baseUrl: Uri): ResponseCookie =
    HeaderReader.asCookie(baseUrl)(WebappstubTheme.cookieName, theme.name)

object WebappstubTheme:
  val name: CIString = CIString("Webappstub-Theme")
  private val cookieName = "webappstub_theme"

  def parse(str: String): ParseResult[WebappstubTheme] =
    UiTheme
      .fromString(str)
      .left
      .map(err => ParseFailure(err, err))
      .map(WebappstubTheme.apply)

  given Header[WebappstubTheme, Header.Single] =
    Header.create(name, _.theme.name, parse)

  def fromRequest[F[_]](req: Request[F]) =
    HeaderReader.from[F, WebappstubTheme](req, Some(cookieName))
