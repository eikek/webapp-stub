package webappstub.store

import webappstub.common.model.*

trait AccountRepo[F[_]]:
  def findByLogin(login: LoginName, state: Option[AccountState]): F[Option[Account]]
  def findById(id: AccountId): F[Option[Account]]
  def insert(account: NewAccount): F[Account]
  def update(id: AccountId, account: NewAccount): F[Unit]
