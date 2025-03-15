package webappstub.common.model

opaque type InviteKey = String

object InviteKey:
  def fromString(s: String): Either[String, InviteKey] =
    if (s.trim.isEmpty()) Left("Empty invitation key not allowed")
    else Right(s)

  def unsafeFromString(s: String): InviteKey =
    fromString(s).fold(sys.error, identity)

  extension (self: InviteKey) def value: String = self
