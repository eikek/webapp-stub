package webappstub.server.common

import cats.data.NonEmptyList
import cats.implicits.*

import webappstub.server.Config

import com.comcast.ip4s.Port
import org.http4s.*
import org.http4s.headers.*
import org.typelevel.ci.CIString

/** Obtain information about the client by inspecting the request. */
object ClientRequestInfo {

  def getBaseUrl[F[_]](cfg: Config, req: Request[F]): Uri =
    if (cfg.baseUrl.renderString.isEmpty)
      getBaseUrl(req, cfg.bindPort).getOrElse(cfg.baseUrl)
    else cfg.baseUrl

  private def getBaseUrl[F[_]](req: Request[F], serverPort: Port): Option[Uri] =
    for {
      schemes <- NonEmptyList.fromList(getProtocol(req).toList)
      scheme <- Uri.Scheme.fromString(schemes.head).toOption
      host <- getHostname(req).flatMap(n => Uri.Host.fromString(n).toOption)
      port = xForwardedPort(req).getOrElse(serverPort.value)
      p = if (port == 80 || port == 443) None else Some(port)
      authority = Uri.Authority(host = host, port = p)
    } yield Uri(scheme = Some(scheme), authority = Some(authority))

  def getHostname[F[_]](req: Request[F]): Option[String] =
    xForwardedHost(req)
      .orElse(xForwardedFor(req))
      .orElse(host(req))

  def getProtocol[F[_]](req: Request[F]): Option[String] =
    xForwardedProto(req).orElse(clientConnectionProto(req))

  private def host[F[_]](req: Request[F]): Option[String] =
    req.headers.get[Host].map(_.host)

  private def xForwardedFor[F[_]](req: Request[F]): Option[String] =
    req.headers
      .get[`X-Forwarded-For`]
      .flatMap(_.values.head)
      .map(_.toInetAddress)
      .flatMap(inet => Option(inet.getHostName).orElse(Option(inet.getHostAddress)))

  private def xForwardedHost[F[_]](req: Request[F]): Option[String] =
    req.headers
      .get(CIString("X-Forwarded-Host"))
      .map(_.head.value)

  private def xForwardedProto[F[_]](req: Request[F]): Option[String] =
    req.headers
      .get(CIString("X-Forwarded-Proto"))
      .map(_.head.value)

  private def clientConnectionProto[F[_]](req: Request[F]): Option[String] =
    req.isSecure.map {
      case true  => "https"
      case false => "http"
    }

  private def xForwardedPort[F[_]](req: Request[F]): Option[Int] =
    req.headers
      .get(CIString("X-Forwarded-Port"))
      .map(_.head.value)
      .flatMap(str => Either.catchNonFatal(str.toInt).toOption)

}
