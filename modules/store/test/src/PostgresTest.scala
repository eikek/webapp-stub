package webappstub.store

import scala.concurrent.duration.Duration

import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*

import webappstub.common.model.Password
import webappstub.store.migration.SchemaMigration
import webappstub.store.postgres.*

import com.comcast.ip4s.*
import org.typelevel.otel4s.trace.Tracer
import skunk.Session
import skunk.implicits.*

trait PostgresTest {
  given Tracer[IO] = Tracer.noop[IO]
  lazy val logger = scribe.cats.io

  def makeSession(cfg: PostgresConfig): Resource[IO, Session[IO]] =
    Session
      .single[IO](
        host = cfg.host.toString,
        port = cfg.port.value,
        user = cfg.user.map(_.username).getOrElse(""),
        password = cfg.user.flatMap(_.password).map(_.value),
        database = cfg.database,
        debug = cfg.debug
      )

  private def fromEnv[A](key: String, f: String => Option[A] = _.some): Option[A] =
    sys.env.get(key).filter(_.nonEmpty).flatMap(f)

  private def currentSystemUser =
    sys.props.get("user.name").getOrElse("")

  private val initConfig = {
    val pgHost = fromEnv("WEBAPPSTUB_TEST_POSTGRES_HOST", Host.fromString)
      .getOrElse(host"localhost")

    val pgPort = fromEnv("WEBAPPSTUB_TEST_POSTGRES_PORT", Port.fromString)
      .getOrElse(port"5432")

    val pgDb = fromEnv("WEBAPPSTUB_TEST_POSTGRES_INIT_DB").getOrElse("postgres")
    val pgUser = fromEnv("WEBAPPSTUB_TEST_POSTGRES_USER")
      .map { user =>
        val pw = fromEnv("WEBAPPSTUB_TEST_POSTGRES_PASSWORD", s => Password(s).some)
        PostgresConfig.User(user, pw)
      }
      .orElse(Some(PostgresConfig.User(currentSystemUser, None)))

    PostgresConfig(pgHost, pgPort, pgDb, pgUser, false, 6, Duration.Zero)
  }

  private val initSession: Resource[IO, Session[IO]] = makeSession(initConfig)

  private val randomDb: Resource[IO, PostgresConfig] =
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
                  initConfig.user
                    .map(u => sql"""CREATE DATABASE "#$db" OWNER #${u.username}""")
                    .getOrElse(sql"""CREATE DATABASE "#$db"""")
                    .command
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
  val accountRepoResource: Resource[IO, AccountRepo[IO]] =
    randomDbWithSchema.map { dbCfg =>
      PostgresAccountRepo(makeSession(dbCfg))
    }
}
