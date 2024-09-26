package webappstub.store.postgres

import java.time.{Instant, ZoneOffset}

import scala.concurrent.duration.Duration

import cats.data.ValidatedNel
import cats.syntax.all.*

import webappstub.common.model.*

import skunk.Codec
import skunk.codec.all as c
import soidc.jwt.JWS

object Codecs:
  val instant: Codec[Instant] =
    c.timestamptz.imap(_.toInstant)(_.atOffset(ZoneOffset.UTC))

  val duration: Codec[Duration] =
    c.interval.imap(jd => Duration.fromNanos(jd.toNanos()))(sd =>
      java.time.Duration.ofNanos(sd.toNanos)
    )

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

  val accountId: Codec[AccountId] =
    c.int8.imap(AccountId(_))(_.value)

  val loginName: Codec[LoginName] =
    c.varchar.eimap(LoginName.fromString)(_.value)

  val password: Codec[Password] =
    c.varchar.imap(Password(_))(_.value)

  val accountState: Codec[AccountState] =
    c.varchar.eimap(AccountState.fromString)(_.name)

  val rememberMeKey: Codec[RememberMeKey] =
    c.varchar.eimap(RememberMeKey.fromString)(_.value)

  val contact: Codec[Contact] =
    (contactId *: accountId *: name *: email.opt *: phoneNumber.opt).to[Contact]

  val provider: Codec[Provider] =
    c.varchar.imap(Provider.apply)(_.value)

  val externalAccountId: Codec[ExternalAccountId] =
    (provider *: c.varchar).to[ExternalAccountId]

  val jws: Codec[JWS] =
    c.varchar.eimap(JWS.fromString)(_.compact)

  val account: Codec[Account] =
    (accountId *: accountState *: loginName *: password *: externalAccountId.opt *: jws.opt *: instant)
      .to[Account]

  val inviteKey: Codec[InviteKey] =
    c.varchar.eimap(InviteKey.fromString)(_.value)

  val inviteRecord: Codec[InviteRecord] =
    (c.int8 *: inviteKey *: instant).to[InviteRecord]

  extension [A](self: Codec[A])
    def vimap[B](fa: A => ValidatedNel[String, B])(fb: B => A): Codec[B] =
      self.eimap(a => fa(a).toEither.leftMap(_.toList.mkString))(fb)
