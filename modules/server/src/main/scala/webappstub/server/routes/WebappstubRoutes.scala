package webappstub.server.routes

import cats.effect.*

import webappstub.backend.Backend
import webappstub.server.Config
import webappstub.server.common.Responses

import org.http4s.HttpRoutes
import org.http4s.implicits.*
import org.http4s.server.Router

object WebappstubRoutes:
  def create[F[_]: Async](backend: Backend[F], config: Config): HttpRoutes[F] =
    Router.of(
      "/app" -> AppRoutes[F](backend, config).routes,
      "/" -> Responses.redirectRoute(uri"/app/contacts")
    )
