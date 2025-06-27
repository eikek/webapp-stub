package webappstub.common.model

import cats.data.{Validated, ValidatedNel}

opaque type Email = String
object Email:
  def apply(email: String): ValidatedNel[String, Email] =
    Validated.condNel(email.contains("@"), email.trim, s"Invalid email: $email")

  def unsafe(email: String): Email =
    apply(email).fold(e => sys.error(e.toList.mkString), identity)

  extension (self: Email)
    def value: String = self
    def contains(s: String) = self.toLowerCase.contains(s)
