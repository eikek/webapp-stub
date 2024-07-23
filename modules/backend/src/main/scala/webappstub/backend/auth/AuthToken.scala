package webappstub.backend.auth

import scala.concurrent.duration.Duration

import cats.effect.*

import webappstub.common.model.AccountId

import scodec.bits.ByteVector

opaque type AuthToken = TokenBase[AccountId]

object AuthToken:
  def of[F[_]: Sync](account: AccountId, key: ByteVector): F[AuthToken] =
    TokenBase.of[F, AccountId](account, key)

  def fromString(s: String): Either[String, AuthToken] =
    TokenBase.fromString[AccountId](s)

  def refresh[F[_]: Sync](token: AuthToken, key: ByteVector): F[AuthToken] =
    of(token.value, key)

  extension (self: AuthToken)
    def value: AccountId = self.value
    def asString: String = self.asString
    def validate[F[_]: Sync](key: ByteVector, validity: Duration): F[Boolean] =
      self.validate(key, validity)
