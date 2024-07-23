package webappstub.backend.auth

import cats.effect.Sync

import webappstub.common.model.RememberMeKey

import scodec.bits.ByteVector

opaque type RememberMeToken = TokenBase[RememberMeKey]

object RememberMeToken:
  def of[F[_]: Sync](token: RememberMeKey, key: ByteVector): F[RememberMeToken] =
    TokenBase.of[F, RememberMeKey](token, key)

  def fromString(s: String): Either[String, RememberMeToken] =
    TokenBase.fromString[RememberMeKey](s)

  extension (self: RememberMeToken)
    def value: RememberMeKey = self.value
    def asString: String = self.asString
    def validate(key: ByteVector): Boolean = self.sigValid(key)
