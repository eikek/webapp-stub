package webappstub.common.model

opaque type Provider = String

object Provider:
  val internal: Provider = "webappstub:internal"

  def apply(p: String): Provider = p

  extension (self: Provider) def value: String = self
