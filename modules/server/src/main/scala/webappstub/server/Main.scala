package webappstub.server

import scala.concurrent.duration.*

import cats.effect.*

import webappstub.backend.Backend
import webappstub.server.common.Responses
import webappstub.server.routes.AppRoutes

import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.Logger as Http4sLogger
import org.http4s.{HttpRoutes, Response, Status}
import org.typelevel.otel4s.trace.Tracer

object Main extends IOApp:
  given Tracer[IO] = Tracer.noop[IO]
  private val logger = scribe.cats.io

  def createRoutes(backend: Backend[IO], config: Config): HttpRoutes[IO] =
    Router(
      "/app" -> AppRoutes(backend, config).routes,
      "/" -> Responses.redirectRoute(uri"/app/contacts")
    )

  def run(args: List[String]): IO[ExitCode] =
    Config.load[IO].flatMap { cfg =>
      logger.info(s"Config: $cfg") >>
        Backend[IO](cfg.backend).use { backend =>
          val routes = createRoutes(backend, cfg)
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
