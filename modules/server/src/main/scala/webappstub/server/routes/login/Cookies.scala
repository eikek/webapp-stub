package webappstub.server.routes.login

import cats.Functor
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.server.Config
import webappstub.server.common.*

import org.http4s.*

object Cookies:
  def remove[F[_]](config: Config)(req: Request[F])(resp: Response[F]) =
    val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
    resp
      .addCookie(AuthCookieName.remove(baseUrl))
      .addCookie(RememberMeCookie.remove(baseUrl))

  def set[F[_]: Functor](
      config: Config
  )(req: Request[F], token: AuthToken, rme: Option[RememberMeToken])(
      resp: F[Response[F]]
  ) =
    val baseUrl = ClientRequestInfo.getBaseUrl(config, req)
    val cookie = AuthCookieName.set(token, baseUrl)
    val rmeCookie = rme
      .map(RememberMeCookie.set(_, baseUrl))
      .getOrElse(RememberMeCookie.remove(baseUrl))
    resp.map(_.addCookie(cookie).addCookie(rmeCookie))
