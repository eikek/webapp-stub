package webappstub.store.postgres

import cats.effect.*
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.store.ContactRepo

import skunk.Session

final class PostgresContactRepo[F[_]: Sync](session: Resource[F, Session[F]])
    extends ContactRepo[F]:
  def selectContacts(
      contains: Option[String],
      limit: Long,
      offset: Long
  ): F[List[Contact]] =
    contains match {
      case Some(q) =>
        session.use(_.execute(ContactSql.findAllContains)((q, offset, limit)))
      case None =>
        session.use(_.execute(ContactSql.findAll)(offset -> limit))
    }

  def findByEmail(email: Email): F[Option[Contact]] =
    session.use(_.option(ContactSql.findByEmail)(email))

  def findById(id: ContactId): F[Option[Contact]] =
    session.use(_.option(ContactSql.findById)(id))

  def insert(c: NewContact): F[ContactId] =
    session.use(_.unique(ContactSql.insert)(c))

  def update(c: Contact): F[Boolean] =
    session.use(_.execute(ContactSql.update)(c.id -> c.withoutId)).as(true)

  def delete(id: ContactId): F[Boolean] =
    session.use(_.execute(ContactSql.delete)(id)).as(true)

  def countAll: F[Long] =
    session.use(_.unique(ContactSql.countAll))
