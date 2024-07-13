package webappstub.server.common

import cats.Applicative
import cats.data.OptionT

import org.http4s.*
import org.http4s.headers.*

trait Responses:

  def redirect[F[_]](uri: Uri, status: Status = Status.SeeOther): Response[F] =
    Response(status = status, headers = Headers(Location(uri)))

  def redirectRoute[F[_]: Applicative](
      uri: Uri,
      status: Status = Status.SeeOther
  ): HttpRoutes[F] =
    HttpRoutes.liftF[F](OptionT.some(redirect(uri, status)))

object Responses extends Responses
