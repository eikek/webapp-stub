package webappstub.server.contacts

import cats.effect.kernel.Sync
import cats.syntax.all.*

import htmx4s.http4s.util.ValidationDsl.*

import webappstub.backend.ContactService
import webappstub.backend.ContactService.UpdateResult
import webappstub.common.model.*
import webappstub.server.contacts.Model.*

trait RoutesApi[F[_]]:
  def upsert(
      form: ContactEditForm,
      id: Option[ContactId]
  ): F[Option[ContactValid[ContactId]]]
  def checkMail(id: Option[ContactId], emailStr: String): F[ContactValid[Email]]
  def delete(id: ContactId): F[Boolean]
  def findById(id: ContactId): F[Option[Contact]]
  def search(query: Option[String], page: Option[Int]): F[List[Contact]]
  def countAll: F[Long]

object RoutesApi:
  def apply[F[_]: Sync](db: ContactService[F]): RoutesApi[F] =
    new RoutesApi[F]:
      def search(query: Option[String], page: Option[Int]): F[List[Contact]] =
        db.search(query, page)
      def countAll: F[Long] = db.count
      def findById(id: ContactId): F[Option[Contact]] = db.findById(id)
      def delete(id: ContactId): F[Boolean] = db.delete(id)
      def upsert(
          form: ContactEditForm,
          id: Option[ContactId]
      ): F[Option[ContactValid[ContactId]]] =
        form
          .toContact(id.getOrElse(ContactId.unknown))
          .fold(
            _.invalid.some.pure[F],
            c =>
              db.upsert(c).map {
                case UpdateResult.Success(id)    => id.valid.some
                case UpdateResult.EmailDuplicate => ContactError.emailExists.some
                case UpdateResult.NotFound       => None
              }
          )

      def checkMail(id: Option[ContactId], emailStr: String): F[ContactValid[Email]] =
        Email(emailStr)
          .keyed(ContactError.Key.Email)
          .fold(
            _.invalid.pure[F],
            email =>
              db.findByEmail(email).map {
                case Some(c) if id.forall(_ != c.id) => ContactError.emailExists
                case _                               => email.valid
              }
          )
