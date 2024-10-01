package webappstub.server.routes.login

import webappstub.common.model.RememberMeToken

import org.http4s.ResponseCookie
import org.http4s.Uri
import soidc.http4s.routes.GetToken
import soidc.http4s.routes.JwtCookie

type RememberMeCookie = "webappstub_remember_me"

object RememberMeCookie:
  val value: RememberMeCookie = "webappstub_remember_me"

  def getToken[F[_]]: GetToken[F] = GetToken.cookie[F](value)

  def remove(uri: Uri): ResponseCookie = JwtCookie.remove(value, uri)

  def set[F[_]](token: RememberMeToken, uri: Uri): ResponseCookie =
    JwtCookie
      .create(value, token.jws, uri)
      .copy(maxAge = token.claims.expirationTime.map(_.toSeconds))
