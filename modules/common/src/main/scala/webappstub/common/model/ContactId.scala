package webappstub.common.model

opaque type ContactId = Long

object ContactId:
  val unknown: ContactId = Long.MinValue

  def apply(n: Long): ContactId = n

  extension (self: ContactId) def value: Long = self
