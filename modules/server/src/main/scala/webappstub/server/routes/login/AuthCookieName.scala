package webappstub.server.routes.login

import webappstub.common.model.AuthToken

import org.http4s.*
import soidc.http4s.routes.JwtCookie

opaque type AuthCookie = "webappstub_auth"

object AuthCookie:
  val value: AuthCookie = "webappstub_auth"

  def remove(uri: Uri): ResponseCookie = JwtCookie.remove(value, uri)

  def set[F[_]](token: AuthToken, uri: Uri): ResponseCookie =
    JwtCookie
      .create(value, token.jws, uri)
      .copy(expires =
        token.claims.expirationTime.map(exp =>
          HttpDate.unsafeFromEpochSecond(exp.toSeconds)
        )
      )

  extension (self: AuthCookie) def asString: String = value
