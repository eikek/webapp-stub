package webappstub.backend

import cats.effect.*
import cats.effect.std.Console
import fs2.io.net.Network

import webappstub.store.*
import webappstub.store.migration.*
import webappstub.store.postgres.*

import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.otel4s.trace.Tracer

trait Backend[F[_]]:
  def config: BackendConfig
  def accountRepo: AccountRepo[F]
  def login: LoginService[F]
  def realms: ConfiguredRealms[F]
  def signup: SignupService[F]
  def contacts: ContactService[F]

object Backend:

  def apply[F[_]: Tracer: Network: Console: Async](
      cfg: BackendConfig
  ): Resource[F, Backend[F]] =
    val logger = scribe.cats.effect[F]
    for
      session <- SkunkSession[F](cfg.database, logger)
      _ <- Resource.eval(session.use(s => SchemaMigration(s).migrate))
      client <- EmberClientBuilder.default[F].build
      contactRepo = new PostgresContactRepo[F](session)
      _accountRepo = new PostgresAccountRepo[F](session)
      _realms <- Resource.eval(ConfiguredRealms[F](cfg.auth, _accountRepo, client))
      _contacts <- ContactService[F](contactRepo)
    yield new Backend[F] {
      val config = cfg
      val contacts = _contacts
      val accountRepo = _accountRepo
      val realms = _realms
      val login = LoginService(cfg.auth, accountRepo, _realms)
      val signup = SignupService(cfg.signup, accountRepo)
    }
