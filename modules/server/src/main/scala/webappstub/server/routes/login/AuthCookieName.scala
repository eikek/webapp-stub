package webappstub.server.routes.login

import webappstub.common.model.AuthToken

import org.http4s.*
import soidc.http4s.routes.JwtCookie

opaque type AuthCookieName = "webappstub_auth"

object AuthCookieName:
  val value: AuthCookieName = "webappstub_auth"

  def remove(uri: Uri): ResponseCookie = JwtCookie.remove(value, uri)

  def set[F[_]](token: AuthToken, uri: Uri): ResponseCookie =
    JwtCookie
      .create(value, token.jws, uri)
      .copy(maxAge = token.claims.expirationTime.map(_.toSeconds))

  extension (self: AuthCookieName) def asString: String = value
