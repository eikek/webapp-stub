package webappstub.common.model

import io.bullet.borer.Decoder
import io.bullet.borer.Encoder

opaque type RememberMeKey = String

object RememberMeKey:
  def fromString(s: String): Either[String, RememberMeKey] =
    if (s.isBlank()) Left(s"Invalid remember-me key: $s")
    else Right(s)

  def unsafeFromString(s: String): RememberMeKey =
    fromString(s).fold(sys.error, identity)

  given Decoder[RememberMeKey] = Decoder.forString
  given Encoder[RememberMeKey] = Encoder.forString

  extension (self: RememberMeKey) def value: String = self
