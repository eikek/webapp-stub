package webappstub.store.postgres

import cats.effect.*
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.store.{ContactQuery, ContactRepo}

import skunk.Session

final class PostgresContactRepo[F[_]: Sync](session: Resource[F, Session[F]])
    extends ContactRepo[F]
    with TxUtil:
  def selectContacts(q: ContactQuery): F[List[Contact]] =
    if (q.query.isEmpty) session.use(_.execute(ContactSql.findAll)(q))
    else session.use(_.execute(ContactSql.findAllContains)(q))

  def findByEmail(account: AccountId, email: Email): F[Option[Contact]] =
    session.use(_.option(ContactSql.findByEmail)(account -> email))

  def findById(id: ContactId): F[Option[Contact]] =
    session.use(_.option(ContactSql.findById)(id))

  def insert(c: NewContact): F[ContactId] =
    session.inTx(_.unique(ContactSql.insert)(c))

  def update(c: Contact): F[Boolean] =
    session.inTx(_.execute(ContactSql.update)(c.id -> c.withoutId)).as(true)

  def delete(id: ContactId): F[Boolean] =
    session.inTx(_.execute(ContactSql.delete)(id)).as(true)

  def countAll(account: AccountId): F[Long] =
    session.use(_.unique(ContactSql.countAll)(account))
