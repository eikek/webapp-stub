package webappstub.common.model

opaque type RememberMeKey = String

object RememberMeKey:
  def fromString(s: String): Either[String, RememberMeKey] =
    if (s.isBlank()) Left(s"Invalid remember-me key: $s")
    else Right(s)

  def unsafeFromString(s: String): RememberMeKey =
    fromString(s).fold(sys.error, identity)

  extension (self: RememberMeKey) def value: String = self
