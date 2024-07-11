package webappstub.store.postgres

import java.time.{Instant, ZoneOffset}

import cats.syntax.all.*
import cats.data.ValidatedNel

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

  extension [A](self: Codec[A])
    def vimap[B](fa: A => ValidatedNel[String, B])(fb: B => A): Codec[B] =
      self.eimap(a => fa(a).toEither.leftMap(_.toList.mkString))(fb)
