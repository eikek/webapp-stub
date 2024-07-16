package webappstub.server.common

import webappstub.backend.auth.AuthToken

import org.http4s.*
import org.typelevel.ci.CIString

final case class WebappstubAuth(token: String):
  def asCookie(baseUrl: Uri): ResponseCookie =
    HeaderReader.asCookie(baseUrl)(WebappstubAuth.cookieName, token)

object WebappstubAuth:
  val name: CIString = CIString("Webappstub-Auth")
  val cookieName = "webappstub_auth"

  def parse(value: String): ParseResult[WebappstubAuth] =
    if (value.isEmpty) ParseResult.fail("Empty auth token", "Empty auth token")
    else Right(WebappstubAuth(value))

  given Header[WebappstubAuth, Header.Single] =
    Header.create(name, _.token, parse)

  def fromRequest[F[_]](req: Request[F]): Option[WebappstubAuth] =
    HeaderReader.from[F, WebappstubAuth](req, Some(cookieName))

  def fromToken(t: AuthToken): WebappstubAuth =
    WebappstubAuth(t.asString)
