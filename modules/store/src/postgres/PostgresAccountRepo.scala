package webappstub.store.postgres

import java.time.Instant

import scala.concurrent.duration.Duration

import cats.effect.*
import cats.effect.std.Random
import cats.syntax.all.*

import webappstub.common.model.*
import webappstub.store.AccountRepo

import scodec.bits.ByteVector
import skunk.Session
import skunk.SqlState
import skunk.data.Completion
import soidc.jwt.JWS

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

  def findByRememberMe(key: RememberMeKey, valid: Duration): F[Option[Account]] =
    session.use(_.option(AccountSql.findByRememberMe)((key, valid, AccountState.Active)))

  def findByExternalId(id: ExternalAccountId): F[Option[Account]] =
    session.use(_.option(AccountSql.findByExternalId)((id, AccountState.Active)))

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

  def setRefreshToken(id: ExternalAccountId, token: JWS): F[Unit] =
    session.inTx(_.execute(AccountSql.updateRefreshToken)(id -> token).void)

  def createInviteKey: F[InviteKey] =
    randomString.flatMap { str =>
      val key = InviteKey.unsafeFromString(str)
      session.use(_.unique(AccountSql.insertNewInvitationKey)(key)).as(key)
    }

  def createRememberMe(id: AccountId): F[RememberMeKey] =
    randomString.flatMap { str =>
      val key = RememberMeKey.unsafeFromString(str)
      session.use(_.unique(AccountSql.insertNewRememberMeKey)(id -> key)).as(key)
    }

  def incrementRememberMeUse(key: RememberMeKey): F[Unit] =
    session.inTx(_.execute(AccountSql.incrementRememberMeUse)(key)).void

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

  def deleteRememberMe(key: RememberMeKey): F[Boolean] =
    session.inTx(_.execute(AccountSql.deleteRememberMe)(key)).map {
      case Completion.Delete(n) => n == 1
      case _                    => false
    }

  def withInvite[A, B](key: InviteKey)(
      f: Option[InviteKey] => F[Either[A, B]]
  ): F[Either[A, B]] =
    session.inTx(_.option(AccountSql.deleteInviteKey2)(key)).flatMap {
      case None => f(None)
      case Some(r) =>
        f(Some(key)).flatMap {
          case b @ Right(_) => b.pure[F]
          case a @ Left(_) =>
            session.use(_.execute(AccountSql.insertInvitation)(r)).as(a)
        }
    }

  private def randomBytes =
    Random.scalaUtilRandom[F].flatMap(_.nextBytes(32)).map(ByteVector.view(_))

  private def randomString =
    randomBytes.map(_.toBase58)
