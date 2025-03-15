package webappstub.store.postgres

import scala.concurrent.duration.*

import cats.effect.*
import cats.effect.std.Console
import cats.syntax.all.*
import fs2.io.net.Network

import webappstub.store.PostgresConfig

import org.typelevel.otel4s.trace.Tracer
import scribe.Scribe
import skunk.Session
import skunk.implicits.*

object SkunkSession {

  def apply[F[_]: Tracer: Network: Console: Temporal](
      cfg: PostgresConfig,
      logger: Scribe[F]
  ): Resource[F, Resource[F, Session[F]]] =
    for
      pool <- apply0(cfg)
      _ <-
        if (cfg.connectRetryDelay > 0.seconds)
          Resource.eval(tryConnect(pool, cfg.connectRetryDelay, logger))
        else Resource.unit
    yield pool

  private val testQuery =
    sql"SELECT 1".query(skunk.codec.all.int4)

  private def tryConnect[F[_]: Temporal](
      pool: Resource[F, Session[F]],
      delay: FiniteDuration,
      logger: Scribe[F]
  ): F[Unit] =
    pool.use(_.unique(testQuery)).void.handleErrorWith { ex =>
      logger.warn("Connecting to Postgres failed! Retrying…", ex) >> Temporal[F]
        .delayBy(tryConnect(pool, delay, logger), delay)
    }

  private def apply0[F[_]: Tracer: Network: Console: Temporal](
      cfg: PostgresConfig
  ): Resource[F, Resource[F, Session[F]]] =
    if (cfg.maxConnections <= 1)
      Resource.pure(
        Session.single(
          host = cfg.host.toString,
          port = cfg.port.value,
          user = cfg.user.map(_.username).getOrElse(""),
          database = cfg.database,
          password = cfg.user.flatMap(_.password).map(_.value),
          debug = cfg.debug
        )
      )
    else
      Session
        .pooled[F](
          host = cfg.host.toString,
          port = cfg.port.value,
          user = cfg.user.map(_.username).getOrElse(""),
          database = cfg.database,
          password = cfg.user.flatMap(_.password).map(_.value),
          debug = cfg.debug,
          max = cfg.maxConnections
        )
}
