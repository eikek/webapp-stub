package webappstub.store.postgres

import cats.effect.*
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.store.AccountRepo

import skunk.Session

final class PostgresAccountRepo[F[_]: Sync](session: Resource[F, Session[F]])
    extends AccountRepo[F]:
  def findByLogin(login: LoginName, state: Option[AccountState]): F[Option[Account]] =
    state match
      case Some(s) =>
        session.use(_.option(AccountSql.findByLoginState)(login -> s))
      case None =>
        session.use(_.option(AccountSql.findByLogin)(login))

  def findById(id: AccountId): F[Option[Account]] =
    session.use(_.option(AccountSql.findById)(id))

  def insert(account: NewAccount): F[Account] =
    session
      .use(_.unique(AccountSql.insert)(account))
      .map { case (id, created) => account.withId(id, created) }

  def update(id: AccountId, account: NewAccount): F[Unit] =
    session.use(_.execute(AccountSql.update)(id -> account)).void
