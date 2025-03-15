package webappstub.store.postgres

import java.time.Instant

import scala.concurrent.duration.Duration

import webappstub.common.model.*
import webappstub.store.postgres.Codecs as c

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*
import soidc.jwt.JWS

private object AccountSql:
  private val cols =
    sql"a.id, s.name, a.login_name, a.password, a.provider, a.external_id, a.refresh_token, a.created_at"

  private val selectStateId =
    sql"""(select id from "account_state" where name = ${c.accountState})"""

  val insert: Query[NewAccount, (AccountId, Instant)] =
    sql"""insert into "account" (state_id, login_name, password, provider, external_id, refresh_token)
    values ($selectStateId, ${c.loginName}, ${c.password}, ${c.externalAccountId.opt}, ${c.jws.opt})
    returning id, created_at
    """
      .query(c.accountId *: c.instant)
      .contrato[NewAccount]

  val update: Command[(AccountId, NewAccount)] =
    sql"""
    update "account"
    set
      state_id = $selectStateId,
      user_name = ${c.loginName},
      password = ${c.password},
    where id = ${c.accountId}
    """.command
      .contramap[(AccountId, NewAccount)] { case (id, nc) =>
        nc.state *: nc.login *: nc.password *: id *: EmptyTuple
      }

  val updateRefreshToken: Command[(ExternalAccountId, JWS)] =
    sql"""
    update "account"
    set
      refresh_token = ${c.jws}
    where
      provider = ${c.provider} AND
      external_id = ${varchar}
    """.command
      .contramap[(ExternalAccountId, JWS)] { case (id, token) =>
        token *: id.provider *: id.id *: EmptyTuple
      }

  val findById: Query[AccountId, Account] =
    sql"""
    select $cols
    from "account" a
    inner join "account_state" s on a.state_id = s.id
    where a.id = ${c.accountId}
    """.query(c.account)

  val findByLogin: Query[LoginName, Account] =
    sql"""
    select $cols
    from "account" a
    inner join "account_state" s on a.state_id = s.id
    where login_name = ${c.loginName}
    """.query(c.account)

  val findByLoginState: Query[(LoginName, AccountState), Account] =
    sql"""
    select $cols
    from "account" a
    inner join "account_state" s on a.state_id = s.id
    where login_name = ${c.loginName} and s.name = ${c.accountState}
    """.query(c.account)

  val findByRememberMe: Query[(RememberMeKey, Duration, AccountState), Account] =
    sql"""
    select $cols
    from "account" a
    inner join "account_state" s on a.state_id = s.id
    inner join "remember_me" r on r.account_id = a.id
    where r.ident = ${c.rememberMeKey}
      AND (r.created_at + ${c.duration}) > now()
      AND s.name = ${c.accountState}
    """.query(c.account)

  val findByExternalId: Query[(ExternalAccountId, AccountState), Account] =
    sql"""
    select $cols
    from "account" a
    inner join "account_state" s on a.state_id = s.id
    where a.provider = ${c.provider}
      AND a.external_id = ${varchar}
      AND s.name = ${c.accountState}
    """
      .query(c.account)
      .contramap[(ExternalAccountId, AccountState)] { case (eId, state) =>
        eId.provider *: eId.id *: state *: EmptyTuple
      }

  val deleteRememberMe: Command[RememberMeKey] =
    sql"""delete from "remember_me" where ident = ${c.rememberMeKey}""".command

  val deleteInviteKey: Command[InviteKey] =
    sql"""delete from "invitation" where "key" = ${c.inviteKey}""".command

  val deleteInviteKey2: Query[InviteKey, InviteRecord] =
    sql"""delete from "invitation" where "key" = ${c.inviteKey} returning id,key,created_at"""
      .query(c.inviteRecord)

  val deleteInviteKeysBefore: Command[Instant] =
    sql"""delete from "invitation" where "created_at" < ${c.instant}""".command

  val insertNewInvitationKey: Query[InviteKey, Long] =
    sql"""insert into "invitation" ("key") values (${c.inviteKey})
          returning id""".query(int8)

  val insertNewRememberMeKey: Query[(AccountId, RememberMeKey), Long] =
    sql"""insert into "remember_me" ("account_id", "ident") values (${c.accountId}, ${c.rememberMeKey}) returning id"""
      .query(int8)

  val incrementRememberMeUse: Command[RememberMeKey] =
    sql"""update "remember_me" set uses = uses + 1 where ident = ${c.rememberMeKey}""".command

  val insertInvitation: Command[InviteRecord] =
    sql"""insert into "invitation" ("id", "key", "created_at") values ($int8, ${c.inviteKey}, ${c.instant})""".command
      .to[InviteRecord]
