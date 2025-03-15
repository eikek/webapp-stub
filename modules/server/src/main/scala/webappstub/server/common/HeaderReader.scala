package webappstub.server.common

import org.http4s.*

/** Get a header value from either the request headers or a cookie */
private object HeaderReader:
  def asCookie(baseUrl: Uri)(name: String, value: String): ResponseCookie = {
    val sec = baseUrl.scheme.exists(_.value.endsWith("s"))
    ResponseCookie(
      name,
      value,
      domain = None,
      path = Some("/app"),
      httpOnly = true,
      secure = sec,
      sameSite = Some(SameSite.Strict)
    )
  }

  def fromHeader[F[_], A](req: Request[F])(using Header.Select[A]) =
    req.headers.get[A]

  def fromCookie[F[_], A](req: Request[F], cookieName: Option[String] = None)(using
      ev: Header[A, ?]
  ) =
    val cname = cookieName.getOrElse(ev.name.toString.toLowerCase().replace('-', '_'))
    req.headers
      .get[headers.Cookie]
      .flatMap(_.values.find(_.name == cname))
      .flatMap(c => ev.parse(c.content).toOption)

  def from[F[_], A](req: Request[F], cookieName: Option[String] = None)(using
      ev: Header[A, Header.Single]
  ) =
    fromHeader(req).orElse(fromCookie(req, cookieName))
