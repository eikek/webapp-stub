package webappstub.server

import scala.concurrent.duration.*

import cats.effect.*

import webappstub.backend.Backend
import webappstub.server.common.Responses
import webappstub.server.config.ScribeConfigure
import webappstub.server.routes.AppRoutes

import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.Router
import org.http4s.server.middleware.Logger as Http4sLogger
import org.typelevel.otel4s.trace.Tracer

object Main extends IOApp:
  given Tracer[IO] = Tracer.noop[IO]
  private val logger = scribe.cats.io

  def createRoutes(backend: Backend[IO], config: Config): IO[HttpRoutes[IO]] =
    AppRoutes(backend, config).routes.map(appRoutes =>
      Router(
        "/app" -> appRoutes,
        "/" -> Responses.redirectRoute(uri"/app/contacts")
      )
    )

  def run(args: List[String]): IO[ExitCode] =
    Config.load[IO].flatMap { cfg =>
      ScribeConfigure.configure[IO](cfg.logConfig) >>
        logger.info(s"Config: $cfg") >>
        Backend[IO](cfg.backend).use { backend =>
          createRoutes(backend, cfg).flatMap { routes =>
            EmberServerBuilder
              .default[IO]
              .withHost(cfg.bindHost)
              .withPort(cfg.bindPort)
              .withHttpApp(
                Http4sLogger.httpApp(
                  logHeaders = true,
                  logBody = false,
                  logAction = Some(msg => logger.trace(msg))
                )(routes.orNotFound)
              )
              .withShutdownTimeout(0.millis)
              .withErrorHandler {
                case ex: DecodeFailure =>
                  logger
                    .warn("Error decoding message!", ex)
                    .as(ex.toHttpResponse(HttpVersion.`HTTP/1.1`))
                case ex =>
                  logger
                    .error("Service raised an error!", ex)
                    .as(Response(status = Status.InternalServerError))
              }
              .build
              .use(_ => IO.never)
              .as(ExitCode.Success)
          }
        }
    }
