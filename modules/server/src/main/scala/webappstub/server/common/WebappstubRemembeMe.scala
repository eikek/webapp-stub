package webappstub.server.common

import webappstub.backend.auth.RememberMeToken

import org.http4s.*
import org.typelevel.ci.CIString

final case class WebappstubRememberMe(token: String):
  def asCookie(baseUrl: Uri): ResponseCookie =
    HeaderReader.asCookie(baseUrl)(WebappstubRememberMe.cookieName, token)

object WebappstubRememberMe:
  val name: CIString = CIString("Webappstub-RememberMe")
  val cookieName = "webappstub_rememberMe"

  def parse(value: String): ParseResult[WebappstubRememberMe] =
    Option(value)
      .filter(_.nonEmpty)
      .toRight(ParseFailure("Invalid remember-me cookie", ""))
      .map(WebappstubRememberMe.apply)

  given Header[WebappstubRememberMe, Header.Single] =
    Header.create(name, _.token, parse)

  def fromRequest[F[_]](req: Request[F]): Option[WebappstubRememberMe] =
    HeaderReader.from[F, WebappstubRememberMe](req, Some(cookieName))

  def fromToken(t: RememberMeToken): WebappstubRememberMe =
    WebappstubRememberMe(t.toString)
