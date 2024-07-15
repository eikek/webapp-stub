package webappstub.store.postgres

import java.time.Instant

import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.store.AccountRepo

import scodec.bits.ByteVector
import skunk.Session
import skunk.SqlState
import skunk.data.Completion

final class PostgresAccountRepo[F[_]: Sync](session: Resource[F, Session[F]])
    extends AccountRepo[F]
    with TxUtil:
  def findByLogin(login: LoginName, state: Option[AccountState]): F[Option[Account]] =
    state match
      case Some(s) =>
        session.use(_.option(AccountSql.findByLoginState)(login -> s))
      case None =>
        session.use(_.option(AccountSql.findByLogin)(login))

  def findById(id: AccountId): F[Option[Account]] =
    session.use(_.option(AccountSql.findById)(id))

  def insert(account: NewAccount): F[Option[Account]] =
    session.inTx(
      _.unique(AccountSql.insert)(account)
        .map { case (id, created) =>
          Some(account.withId(id, created))
        }
        .recover { case SqlState.UniqueViolation(_) =>
          None
        }
    )

  def update(id: AccountId, account: NewAccount): F[Unit] =
    session.inTx(_.execute(AccountSql.update)(id -> account).void)

  def createInviteKey: F[InviteKey] =
    Random.scalaUtilRandom[F].flatMap(_.nextBytes(32)).flatMap { bytes =>
      val key = InviteKey.unsafeFromString(ByteVector.view(bytes).toBase58)
      session.use(_.unique(AccountSql.insertInvitationKey)(key)).as(key)
    }

  def deleteInviteKeys(fromBefore: Instant): F[Long] =
    session.inTx(_.execute(AccountSql.deleteInviteKeysBefore)(fromBefore)).map {
      case Completion.Delete(n) => n.toLong
      case _                    => 0L
    }

  def deleteInviteKey(key: InviteKey): F[Boolean] =
    session.inTx(_.execute(AccountSql.deleteInviteKey)(key)).map {
      case Completion.Delete(n) => n == 1
      case _                    => false
    }
