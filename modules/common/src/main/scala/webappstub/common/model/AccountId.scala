package webappstub.common.model

import io.bullet.borer.Decoder
import io.bullet.borer.Encoder

opaque type AccountId = Long

object AccountId:
  def apply(n: Long): AccountId = n
  def fromString(s: String): Either[String, AccountId] =
    s.toLongOption.map(apply).toRight(s"Invalid account id: $s")

  given Encoder[AccountId] = Encoder.forLong
  given Decoder[AccountId] = Decoder.forLong

  extension (self: AccountId) def value: Long = self
