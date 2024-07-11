package webappstub.store

import cats.effect.*

import webappstub.common.model.*
import webappstub.store.postgres.PostgresContactRepo
import skunk.Session

trait ContactRepo[F[_]]:
  def selectContacts(contains: Option[String], limit: Long, offset: Long): F[List[Contact]]
  def findByEmail(email: Email): F[Option[Contact]]
  def findById(id: ContactId): F[Option[Contact]]
  def insert(c: NewContact): F[ContactId]
  def update(c: Contact): F[Boolean]
  def delete(id: ContactId): F[Boolean]
  def countAll: F[Long]

object ContactRepo:
  def create[F[_]: Async](session: Resource[F, Session[F]]): Resource[F, ContactRepo[F]] =
    Resource.pure(PostgresContactRepo[F](session))
