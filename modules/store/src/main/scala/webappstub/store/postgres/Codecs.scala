package webappstub.store.postgres

import java.time.{Instant, ZoneOffset}

import cats.data.ValidatedNel
import cats.syntax.all.*

import webappstub.common.model.*

import skunk.Codec
import skunk.codec.all as c

object Codecs:
  val instant: Codec[Instant] =
    c.timestamptz.imap(_.toInstant)(_.atOffset(ZoneOffset.UTC))

  val contactId: Codec[ContactId] =
    c.int8.imap(ContactId(_))(_.value)

  val email: Codec[Email] =
    c.varchar.vimap(Email.apply)(_.value)

  val phoneNumber: Codec[PhoneNumber] =
    c.varchar.vimap(PhoneNumber.apply)(_.value)

  val name: Codec[Name] =
    (c.varchar *: c.varchar)
      .to[(String, String)]
      .vimap(t => Name.create(t._1, t._2))(n => (n.first, n.last))

  val contact: Codec[Contact] =
    (contactId *: name *: email.opt *: phoneNumber.opt).to[Contact]

  val accountId: Codec[AccountId] =
    c.int8.imap(AccountId(_))(_.value)

  val loginName: Codec[LoginName] =
    c.varchar.eimap(LoginName.fromString)(_.value)

  val password: Codec[Password] =
    c.varchar.imap(Password(_))(_.value)

  val accountState: Codec[AccountState] =
    c.varchar.eimap(AccountState.fromString)(_.name)

  val account: Codec[Account] =
    (accountId *: accountState *: loginName *: password *: instant).to[Account]

  extension [A](self: Codec[A])
    def vimap[B](fa: A => ValidatedNel[String, B])(fb: B => A): Codec[B] =
      self.eimap(a => fa(a).toEither.leftMap(_.toList.mkString))(fb)
