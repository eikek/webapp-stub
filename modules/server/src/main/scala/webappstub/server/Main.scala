package webappstub.server

import scala.concurrent.duration.*

import cats.effect.*

import htmx4s.http4s.WebjarRoute

import org.http4s.{HttpRoutes, Response, Status}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.Logger as Http4sLogger
import webappstub.backend.Backend
import org.typelevel.otel4s.trace.Tracer

object Main extends IOApp:
  given Tracer[IO] = Tracer.noop[IO]
  private val logger = scribe.cats.io

  // refers to our own js and css stuff, version is not needed
  private val selfWebjar = WebjarRoute.Webjar("self")("webappstub-server", "", "")
  def createRoutes(backend: Backend[IO]): HttpRoutes[IO] = Router.of(
    "/assets" -> WebjarRoute.withHtmx[IO](selfWebjar).serve,
    "/ui" -> contacts.Routes[IO](contacts.RoutesApi(backend.contacts)).routes
  )

  def run(args: List[String]): IO[ExitCode] =
    Config.load[IO].flatMap { cfg =>
      Backend[IO](cfg.backend).use { backend =>
        val routes = createRoutes(backend)
        EmberServerBuilder
          .default[IO]
          .withHost(cfg.bindHost)
          .withPort(cfg.bindPort)
          .withHttpApp(
            Http4sLogger.httpApp(true, false)(routes.orNotFound)
          )
          .withShutdownTimeout(0.millis)
          .withErrorHandler { case ex =>
            logger
              .error("Service raised an error!", ex)
              .as(Response(status = Status.InternalServerError))
          }
          .build
          .use(_ => IO.never)
          .as(ExitCode.Success)
      }
    }
