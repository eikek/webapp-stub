package webappstub.common.model

opaque type LoginName = String

object LoginName:
  private val nameRegex = "[a-zA-Z0-9_\\-@\\.]+".r
  val autoUser: LoginName = "auto_user"

  def fromString(s: String): Either[String, LoginName] =
    if (nameRegex.matches(s)) Right(s)
    else Left(s"Invalid username: $s")

  extension (self: LoginName) def value: String = self
