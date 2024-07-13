package webappstub.common.model

opaque type AccountId = Long

object AccountId:
  def apply(n: Long): AccountId = n
  def fromString(s: String): Either[String, AccountId] =
    s.toLongOption.map(apply).toRight(s"Invalid account id: $s")

  extension (self: AccountId) def value: Long = self
