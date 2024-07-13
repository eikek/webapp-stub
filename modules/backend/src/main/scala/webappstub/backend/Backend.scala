package webappstub.backend

import cats.effect.*
import cats.effect.std.Console
import fs2.io.net.Network

import webappstub.store.*
import webappstub.store.migration.*
import webappstub.store.postgres.*

import org.typelevel.otel4s.trace.Tracer

trait Backend[F[_]]:
  def config: BackendConfig
  def login: LoginService[F]
  def contacts: ContactService[F]

object Backend:

  def apply[F[_]: Tracer: Network: Console: Async](
      cfg: BackendConfig
  ): Resource[F, Backend[F]] =
    for
      session <- SkunkSession[F](cfg.database)
      _ <- Resource.eval(session.use(s => SchemaMigration(s).migrate))
      contactRepo = new PostgresContactRepo[F](session)
      accountRepo = new PostgresAccountRepo[F](session)
      _contacts <- ContactService[F](contactRepo)
    yield new Backend[F] {
      val config = cfg
      val contacts = _contacts
      val login = LoginService(cfg.auth, accountRepo)
    }
