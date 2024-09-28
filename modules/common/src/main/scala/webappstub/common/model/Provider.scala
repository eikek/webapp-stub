package webappstub.common.model

import soidc.jwt.StringOrUri

opaque type Provider = String

object Provider:
  val internal: Provider = "webappstub:internal"

  def apply(p: String): Provider = p

  extension (self: Provider)
    def value: String = self
    def uri: StringOrUri = StringOrUri(self)
