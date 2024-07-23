package webappstub.store

import java.time.Instant

import scala.concurrent.duration.Duration

import webappstub.common.model.*

trait AccountRepo[F[_]]:
  def findByLogin(login: LoginName, state: Option[AccountState]): F[Option[Account]]
  def findById(id: AccountId): F[Option[Account]]
  def findByRememberMe(key: RememberMeKey, valid: Duration): F[Option[Account]]
  def insert(account: NewAccount): F[Option[Account]]
  def update(id: AccountId, account: NewAccount): F[Unit]
  def createInviteKey: F[InviteKey]
  def createRememberMe(id: AccountId): F[RememberMeKey]
  def incrementRememberMeUse(key: RememberMeKey): F[Unit]
  def deleteRememberMe(key: RememberMeKey): F[Boolean]
  def deleteInviteKeys(fromBefore: Instant): F[Long]
  def deleteInviteKey(key: InviteKey): F[Boolean]
  def withInvite[A, B](key: InviteKey)(
      f: Option[InviteKey] => F[Either[A, B]]
  ): F[Either[A, B]]
