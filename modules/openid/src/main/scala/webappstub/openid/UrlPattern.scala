package webappstub.openid

opaque type UrlPattern = String

object UrlPattern:

  def fromString(str: String): Either[String, UrlPattern] = Right(str)
  def unsafeFromString(str: String): UrlPattern =
    fromString(str).fold(sys.error, identity)

  extension (self: UrlPattern)
    def render: String = self
    def matches(str: String): Boolean = true
