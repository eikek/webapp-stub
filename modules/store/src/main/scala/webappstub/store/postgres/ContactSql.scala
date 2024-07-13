package webappstub.store.postgres

import webappstub.common.model.*
import webappstub.store.postgres.Codecs as c

import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

private object ContactSql:

  val insert: Query[NewContact, ContactId] =
    sql"""insert into contact (first_name,last_name,email,phone)
    values (${c.name}, ${c.email.opt}, ${c.phoneNumber.opt})
    returning id
    """
      .query(c.contactId)
      .contrato[NewContact]

  val update: Command[(ContactId, NewContact)] =
    sql"""
    update "contact"
    set
      first_name = ${varchar},
      last_name = ${varchar},
      email = ${c.email.opt},
      phone = ${c.phoneNumber.opt}
    where id = ${c.contactId}
    """.command
      .contramap[(ContactId, NewContact)] { case (id, nc) =>
        nc.name.first *: nc.name.last *: nc.email *: nc.phone *: id *: EmptyTuple
      }

  val delete: Command[ContactId] =
    sql"""
    delete from "contact" where "id" = ${c.contactId}
    """.command

  val countAll: Query[skunk.Void, Long] =
    sql"""select count(id) from "contact"""".query(int8)

  private val contactCols = sql"""c.id, c.first_name, c.last_name, c.email, c.phone"""

  def findById: Query[ContactId, Contact] =
    sql"""
    select $contactCols
    from "contact" c
    where c.id = ${c.contactId}
    """.query(c.contact)

  def findByEmail: Query[Email, Contact] =
    sql"""
    select $contactCols
    from "contact" c
    where c.email = ${c.email}
    """.query(c.contact)

  def findAll: Query[(Long, Long), Contact] =
    sql"""
    select $contactCols
    from "contact" c
    order by c.last_name, c.first_name
    offset $int8 limit $int8
    """
      .query(c.contact)

  def findAllContains: Query[(String, Long, Long), Contact] =
    sql"""
    select $contactCols
    from "contact" c
    where lower(c.first_name) like $varchar or
      lower(c.last_name) like $varchar or
      lower(c.email) like $varchar or
      lower(c.phone) like $varchar
    order by c.last_name, c.first_name
    offset $int8 limit $int8
    """
      .query(c.contact)
      .contramap[(String, Long, Long)] { case (q, off, limit) =>
        q *: q *: q *: q *: off *: limit *: EmptyTuple
      }
