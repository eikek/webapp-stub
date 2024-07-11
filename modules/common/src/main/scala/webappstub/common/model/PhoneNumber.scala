package webappstub.common.model

import cats.data.{Validated, ValidatedNel}

opaque type PhoneNumber = String
object PhoneNumber:
  def apply(num: String): ValidatedNel[String, PhoneNumber] =
    Validated.condNel(num.nonEmpty, num, s"Invalid phone number: $num")

  extension (self: PhoneNumber)
    def value: String = self
    def contains(s: String) = self.toLowerCase.contains(s)
