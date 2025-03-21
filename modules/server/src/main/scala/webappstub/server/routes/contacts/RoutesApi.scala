package webappstub.server.routes.contacts

import cats.effect.kernel.Sync
import cats.syntax.all.*

import webappstub.backend.ContactService
import webappstub.backend.ContactService.UpdateResult
import webappstub.common.model.*
import webappstub.server.routes.contacts.Model.*

import htmx4s.http4s.util.ValidationDsl.*

trait RoutesApi[F[_]]:
  def upsert(
      form: ContactEditForm,
      owner: AccountId,
      id: Option[ContactId]
  ): F[Option[ContactValid[ContactId]]]
  def checkMail(
      id: Option[ContactId],
      account: AccountId,
      emailStr: String
  ): F[ContactValid[Email]]
  def delete(id: ContactId): F[Boolean]
  def findById(id: ContactId, account: AccountId): F[Option[Contact]]
  def search(
      account: AccountId,
      query: Option[String],
      page: Option[Int]
  ): F[List[Contact]]
  def countAll(account: AccountId): F[Long]

object RoutesApi:
  def apply[F[_]: Sync](db: ContactService[F]): RoutesApi[F] =
    new RoutesApi[F]:
      def search(
          account: AccountId,
          query: Option[String],
          page: Option[Int]
      ): F[List[Contact]] =
        db.search(account, query, page)
      def countAll(account: AccountId): F[Long] = db.count(account)
      def findById(id: ContactId, account: AccountId): F[Option[Contact]] =
        db.findById(id).map(_.filter(_.owner == account))
      def delete(id: ContactId): F[Boolean] = db.delete(id)
      def upsert(
          form: ContactEditForm,
          owner: AccountId,
          id: Option[ContactId]
      ): F[Option[ContactValid[ContactId]]] =
        form
          .toContact(id.getOrElse(ContactId.unknown), owner)
          .fold(
            _.invalid.some.pure[F],
            c =>
              db.upsert(c).map {
                case UpdateResult.Success(id)    => id.valid.some
                case UpdateResult.EmailDuplicate => ContactError.emailExists.some
                case UpdateResult.NotFound       => None
              }
          )

      def checkMail(
          id: Option[ContactId],
          account: AccountId,
          emailStr: String
      ): F[ContactValid[Email]] =
        Email(emailStr)
          .keyed(ContactError.Key.Email)
          .fold(
            _.invalid.pure[F],
            email =>
              db.findByEmail(account, email).map {
                case Some(c) if id.forall(_ != c.id) => ContactError.emailExists
                case _                               => email.valid
              }
          )
