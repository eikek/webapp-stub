package webappstub.backend

import cats.Monad
import cats.effect.*
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.store.*

trait ContactService[F[_]]:
  def search(
      account: AccountId,
      query: Option[String],
      page: Option[Int]
  ): F[List[Contact]]
  def delete(id: ContactId): F[Boolean]
  def upsert(contact: Contact): F[ContactService.UpdateResult]
  def findById(id: ContactId): F[Option[Contact]]
  def findByEmail(account: AccountId, email: Email): F[Option[Contact]]
  def count(account: AccountId): F[Long]

object ContactService:
  enum UpdateResult:
    case Success(id: ContactId)
    case NotFound
    case EmailDuplicate

  def apply[F[_]: Monad](repo: ContactRepo[F]): Resource[F, ContactService[F]] =
    Resource.pure(new ContactService[F]:
      def search(
          account: AccountId,
          query: Option[String],
          page: Option[Int]
      ): F[List[Contact]] =
        val q = query.map(_.toLowerCase).map(e => s"%$e%")
        val skip = (page.getOrElse(1) - 1) * 10
        val cq = ContactQuery(account, q.orEmpty, 10, skip)
        repo.selectContacts(cq)

      def count(account: AccountId): F[Long] = repo.countAll(account)

      def delete(id: ContactId): F[Boolean] =
        repo.delete(id)

      def upsert(contact: Contact): F[UpdateResult] =
        contact.email
          .flatTraverse(repo.findByEmail(contact.owner, _))
          .flatMap:
            case Some(existing) =>
              if (existing.id != contact.id) UpdateResult.EmailDuplicate.pure[F]
              else
                repo
                  .update(contact)
                  .map:
                    case true  => UpdateResult.Success(contact.id)
                    case false => UpdateResult.NotFound
            case None =>
              repo.insert(contact.withoutId).map(UpdateResult.Success(_))

      def findByEmail(account: AccountId, email: Email): F[Option[Contact]] =
        repo.findByEmail(account, email)

      def findById(id: ContactId): F[Option[Contact]] =
        repo.findById(id)
    )
