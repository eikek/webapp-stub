package webappstub.backend.auth

import cats.effect.Sync
import scodec.bits.ByteVector
import scala.concurrent.duration.Duration
import webappstub.common.model.RememberMeKey

opaque type RememberMeToken = TokenBase[RememberMeKey]

object RememberMeToken:
  def of[F[_]: Sync](token: RememberMeKey, key: ByteVector): F[RememberMeToken] =
    TokenBase.of[F, RememberMeKey](token, key)

  def fromString(s: String): Either[String, RememberMeToken] =
    TokenBase.fromString[RememberMeKey](s)

  extension (self: RememberMeToken)
    def asString: String = self.asString
    def validate[F[_]: Sync](key: ByteVector, validity: Duration): F[Boolean] =
      self.validate(key, validity)
