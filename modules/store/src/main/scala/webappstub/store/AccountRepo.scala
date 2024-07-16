package webappstub.store

import java.time.Instant

import webappstub.common.model.*

trait AccountRepo[F[_]]:
  def findByLogin(login: LoginName, state: Option[AccountState]): F[Option[Account]]
  def findById(id: AccountId): F[Option[Account]]
  def insert(account: NewAccount): F[Option[Account]]
  def update(id: AccountId, account: NewAccount): F[Unit]
  def createInviteKey: F[InviteKey]
  def deleteInviteKeys(fromBefore: Instant): F[Long]
  def deleteInviteKey(key: InviteKey): F[Boolean]
  def withInvite[A, B](key: InviteKey)(
      f: Option[InviteKey] => F[Either[A, B]]
  ): F[Either[A, B]]
