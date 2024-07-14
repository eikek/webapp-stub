package webappstub.server.common

import cats.Monad
import cats.data.OptionT
import cats.syntax.all.*

import webappstub.server.data.UiTheme
import webappstub.server.routes.Layout

import htmx4s.http4s.Htmx4sDsl
import org.http4s.*
import org.http4s.headers.*

trait Responses:

  def redirect[F[_]](uri: Uri, status: Status = Status.SeeOther): Response[F] =
    Response(status = status, headers = Headers(Location(uri)))

  def redirectRoute[F[_]: Monad](
      uri: Uri,
      status: Status = Status.SeeOther
  ): HttpRoutes[F] = {
    val dsl = new Htmx4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] { case GET -> Root => redirect(uri, status).pure[F] }
  }

  def notFoundHtmlRoute[F[_]: Monad]: HttpRoutes[F] = {
    val dsl = new Htmx4sDsl[F] {}
    import dsl.*
    import org.http4s.scalatags.*

    HttpRoutes { req =>
      val theme = WebappstubTheme.fromRequest(req).map(_.theme).getOrElse(UiTheme.Light)
      OptionT.liftF(NotFound(Layout.notFoundPage(theme)))
    }
  }

object Responses extends Responses
