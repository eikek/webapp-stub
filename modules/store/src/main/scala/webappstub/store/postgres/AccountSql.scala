package webappstub.store.postgres

import java.time.Instant

import webappstub.common.model.*
import webappstub.store.postgres.Codecs as c

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

private object AccountSql:
  private val cols = sql"a.id, s.name, a.login_name, a.password, a.created_at"

  private val selectStateId =
    sql"""(select id from "account_state" where name = ${c.accountState})"""

  val insert: Query[NewAccount, (AccountId, Instant)] =
    sql"""insert into "account" (state_id, login_name, password)
    values ($selectStateId, ${c.loginName}, ${c.password})
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

  val findById: Query[AccountId, Account] =
    sql"""
    select $cols
    from "account" a
    inner join "account_state" s on a.state_id = s.id
    where id = ${c.accountId}
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

  val deleteInviteKey: Command[InviteKey] =
    sql"""delete from "invitation" where "key" = ${c.inviteKey}""".command

  val deleteInviteKeysBefore: Command[Instant] =
    sql"""delete from "invitation" where "created_at" < ${c.instant}""".command

  val insertInvitationKey: Query[InviteKey, Long] =
    sql"""insert into "invitation" ("key") values (${c.inviteKey})
          returning id""".query(int8)
