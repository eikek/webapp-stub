package webappstub.store

import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*

import webappstub.store.migration.SchemaMigration
import webappstub.common.Password

import com.comcast.ip4s.*
import org.typelevel.otel4s.trace.Tracer
import skunk.Session
import skunk.implicits.*
import webappstub.store.postgres.PostgresContactRepo

trait PostgresTest {
  given Tracer[IO] = Tracer.noop[IO]
  lazy val logger = scribe.cats.io

  def makeSession(cfg: PostgresConfig): Resource[IO, Session[IO]] =
    Session
      .single[IO](
        host = cfg.host.toString,
        port = cfg.port.value,
        user = cfg.user,
        password = cfg.password.value.some,
        database = cfg.database,
        debug = cfg.debug
      )

  private val initConfig =
    PostgresConfig(
      host"wasdev",
      port"5432",
      "webappstub",
      "dev",
      Password("dev"),
      false,
      6
    )

  private val initSession: Resource[IO, Session[IO]] = makeSession(initConfig)

  private val randomDb =
    for {
      rand <- Random.scalaUtilRandom[IO].toResource
      db <- rand.nextAlphaNumeric.replicateA(9).map(c => ('d' :: c).mkString).toResource
      newCfg = initConfig.copy(database = db)
      createDb <-
        Resource.make(
          logger.debug(s"Creating test database: $db") *>
            initSession
              .use(
                _.execute(
                  sql"""CREATE DATABASE "#$db" OWNER #${initConfig.user}""".command
                )
              )
              .as(newCfg)
        )(_ =>
          logger.debug(s"Drop database $db") *> initSession
            .use(_.execute(sql"""DROP DATABASE "#$db"""".command))
            .void
        )
    } yield createDb

  private val randomDbWithSchema =
    randomDb.evalTap(dbCfg =>
      makeSession(dbCfg.copy(maxConnections = 1)).use(s => SchemaMigration(s).migrate)
    )

  // Connects to a random-named database
  val session: Resource[IO, Session[IO]] =
    randomDb.flatMap(cfg => makeSession(cfg).evalTap(s => SchemaMigration(s).migrate))

  val contactRepoResource: Resource[IO, ContactRepo[IO]] =
    randomDbWithSchema.map { dbCfg =>
      PostgresContactRepo(makeSession(dbCfg))
    }
}
